package com.dandi.nyummy.meal.service

import com.dandi.nyummy.infra.ai.nutrition.NutritionAnalysisClient
import com.dandi.nyummy.infra.aws.s3.S3Presigner
import com.dandi.nyummy.meal.dto.*
import com.dandi.nyummy.meal.entity.Meal
import com.dandi.nyummy.meal.enum.DailyNutritionEvaluation
import com.dandi.nyummy.meal.enum.MealStatus
import com.dandi.nyummy.meal.mapper.toEntity
import com.dandi.nyummy.meal.mapper.toGetStatusResponse
import com.dandi.nyummy.meal.repository.MealRepository
import com.dandi.nyummy.profile.repository.ProfileRepository
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@Service
class MealService(
    private val analysisService: AnalysisService,
    private val mealRepository: MealRepository,
    private val s3Presigner: S3Presigner,
    private val clock: Clock = Clock.System,
    private val profileRepository: ProfileRepository,
    private val nutritionRecommendationCalculator: NutritionRecommendationCalculator,
    private val dailyNutritionEvaluationCalculator: DailyNutritionEvaluationCalculator,
    private val nutritionAnalysisClient: NutritionAnalysisClient
) {
    companion object {
        private val PRESIGNED_PUT_URL_EXPIRATION = 10.minutes
        private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp")
    }

    fun createUploadUrl(request: UploadImageRequest): UploadImageResponse {

        val extension = request.fileName.substringAfterLast(".", missingDelimiterValue = "").lowercase()

        require(extension in ALLOWED_EXTENSIONS) {
            "지원하지 않는 파일 형식입니다. (허용: $ALLOWED_EXTENSIONS)"
        }

        require(request.contentType.startsWith("image/")) {
            "이미지 타입만 업로드 가능합니다."
        }

        require(0 < request.fileSizeBytes && request.fileSizeBytes <= 10_485_760) {
            "파일 크기는 최소 0보다 크며, 최대 10MB(10_485_760 Bytes)를 초과할 수 없습니다."
        }

        val s3KeyPath = "meal/${UUID.randomUUID()}.$extension"
        val expirationInstant = clock.now() + PRESIGNED_PUT_URL_EXPIRATION

        val uploadUrl = runCatching {
            s3Presigner.getPutObjectUrl(
                keyName = s3KeyPath,
                type = request.contentType,
                duration = PRESIGNED_PUT_URL_EXPIRATION
            )
        }.getOrElse {
            println("PUT url 반환 실패")
            throw it
        }

        return UploadImageResponse(
            uploadUrl = uploadUrl.toString(),
            imageKey = s3KeyPath,
            uploadMethod = "PUT",
            uploadHeaders = mapOf("Content-Type" to request.contentType),
            expiresAt = expirationInstant,
        )
    }

    @Transactional
    fun createMeal(request: CreateMealRequest): GetStatusResponse {
        val savedMeal = mealRepository.save(request.toEntity())

        analysisService.analyzeNutrition(savedMeal.id)

        return savedMeal.toGetStatusResponse()
    }

    fun getDailyMeals(userId: Long, year: Int, month: Int, day: Int): DailyMealsResponse {

        val zone = ZoneId.of("Asia/Seoul")
        val date = LocalDate.of(year, month, day)
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()

        val mealsByPeriod = mealRepository.getMealsByUserIdAndPeriod(userId, start, end)

        val meals = mutableListOf<DailyMealResponse>()

        var totalCalory: Int = 0
        var totalCarbs: Int = 0
        var totalProtein: Int = 0
        var totalFat: Int = 0

        for (meal in mealsByPeriod) {

            meals.add(
                DailyMealResponse(
                    mealId = meal.id,
                    name = meal.name,
                    mealAt = meal.mealAt,
                    calory = meal.calory ?: 0,
                    carbs = meal.carbs ?: 0,
                    protein = meal.protein ?: 0,
                    fat = meal.fat ?: 0,
                    status = meal.status
                )
            )

            totalCalory += meal.calory ?: 0
            totalCarbs += meal.carbs ?: 0
            totalProtein += meal.protein ?: 0
            totalFat += meal.fat ?: 0
        }

        val profile = profileRepository.getProfileByUserId(userId)

        val recommended =
            nutritionRecommendationCalculator.calculateRecommendedDailyIntake(profile, LocalDate.of(year, month, day))

        val dailyNutrition = DailyNutritionResponse(
            current = Nutrition(
                calory = totalCalory,
                carbs = totalCarbs,
                protein = totalProtein,
                fat = totalFat,
            ),
            target = Nutrition(
                calory = recommended.calory,
                carbs = recommended.carbs,
                protein = recommended.protein,
                fat = recommended.fat
            )
        )

        return DailyMealsResponse(
            date = LocalDate.of(year, month, day),
            meals = meals,
            dailyNutrition = dailyNutrition
        )
    }

    fun calculateDailyNutritionEvaluation(
        meals: List<Meal>,
        recommended: RecommendedDailyIntake,
    ): DailyNutritionEvaluation {

        if (meals.isEmpty()) {
            return DailyNutritionEvaluation.UNRECORDED
        }

        var totalCalory: Int = 0
        var totalCarbs: Int = 0
        var totalProtein: Int = 0
        var totalFat: Int = 0

        for (meal in meals) {
            totalCalory += meal.calory ?: 0
            totalCarbs += meal.carbs ?: 0
            totalProtein += meal.protein ?: 0
            totalFat += meal.fat ?: 0
        }

        val recommendedCalory = recommended.calory
        val recommendedCarbs = recommended.carbs
        val recommendedProtein = recommended.protein
        val recommendedFat = recommended.fat

        if (isPositive(
                totalCalory, recommendedCalory,
                totalCarbs, recommendedCarbs,
                totalProtein, recommendedProtein,
                totalFat, recommendedFat
            )
        ) {

            return DailyNutritionEvaluation.POSITIVE
        }

        return DailyNutritionEvaluation.NEGATIVE
    }

    private fun isPositive(
        totalCalory: Int, recommendedCalory: Int,
        totalCarbs: Int, recommendedCarbs: Int,
        totalProtein: Int, recommendedProtein: Int,
        totalFat: Int, recommendedFat: Int
    ): Boolean = isPositiveNutrition(totalCalory, recommendedCalory) &&
            isPositiveNutrition(totalCarbs, recommendedCarbs) &&
            isPositiveNutrition(totalProtein, recommendedProtein) &&
            isPositiveNutrition(totalFat, recommendedFat)

    fun isPositiveNutrition(totalValue: Int, recommendedValue: Int): Boolean {
        return recommendedValue * 0.9 <= totalValue && totalValue < recommendedValue * 1.5
    }

    fun getMonthlyMeals(userId: Long, year: Int, month: Int): MonthlyMealsResponse {

        val zone = ZoneId.of("Asia/Seoul")
        val range = MonthlyCalendarRange.calculate(YearMonth.of(year, month))

        val mealsByPeriod = mealRepository.getMealsByUserIdAndPeriod(
            userId,
            range.startDate.atStartOfDay(zone).toInstant(),
            range.endDate.plusDays(1).atStartOfDay(zone).toInstant()
        )


        val mealsByDate: Map<LocalDate, List<Meal>> =
            mealsByPeriod
                .groupBy { it.mealAt.atZone(zone).toLocalDate() }


        val profile = profileRepository.getProfileByUserId(userId)
        val recommended = nutritionRecommendationCalculator.calculateRecommendedDailyIntake(profile, LocalDate.now())

        val days = mutableListOf<MonthlyMealDayResponse>()
        var date = range.startDate
        while (date <= range.endDate) {

            days.add(
                MonthlyMealDayResponse(
                    date = date,
                    isCurrentMonth = date.year == year && date.monthValue == month,
                    dailyNutritionEvaluation = dailyNutritionEvaluationCalculator.calculateDailyNutritionEvaluation(
                        meals = mealsByDate[date] ?: emptyList(),
                        recommended = recommended
                    ),
                    foodIconIds = emptyList()
                )
            )
            date = date.plusDays(1)
        }

        return MonthlyMealsResponse(
            year = year,
            month = month,
            days = days
        )
    }

    fun getMeal(userId: Long, mealId: Long): MealResponse {

        val meal = mealRepository.getMealByIdAndUserIdAndDeletedAtIsNull(mealId, userId)
            ?: throw Exception("Meal Not Found")

        val imageUrl = s3Presigner.getGetObjectUrl(meal.imageKey, 10.minutes)

        return MealResponse(
            mealId = mealId,
            name = meal.name,
            mealAt = meal.mealAt,
            status = meal.status,
            nutrition = Nutrition(
                calory = meal.calory ?: 0,
                carbs = meal.carbs ?: 0,
                protein = meal.protein ?: 0,
                fat = meal.fat ?: 0,
            ),
            imageUrl = imageUrl
        )
    }

    @Transactional
    fun updateMeal(userId: Long, mealId: Long, name: String): MealResponse {

        val meal = mealRepository.getMealByIdAndUserIdAndDeletedAtIsNull(mealId, userId)
            ?: throw Exception("Meal Not Found")

        val imageUrl = s3Presigner.getGetObjectUrl(meal.imageKey, 10.minutes)

        meal.updateName(name)

        return MealResponse(
            mealId = mealId,
            name = meal.name,
            mealAt = meal.mealAt,
            status = meal.status,
            nutrition = Nutrition(
                calory = meal.calory ?: 0,
                carbs = meal.carbs ?: 0,
                protein = meal.protein ?: 0,
                fat = meal.fat ?: 0
            ),
            imageUrl = imageUrl
        )
    }

    @Transactional
    fun deleteMeal(userId: Long, mealId: Long) {

        val meal = mealRepository.getMealByIdAndUserIdAndDeletedAtIsNull(mealId, userId)
            ?: throw Exception("Meal Not Found")

        meal.updateDeletedAt(Instant.now())
    }

    @Transactional
    fun updateStatus(mealId: Long, status: MealStatus) {

        val meal = mealRepository.findByIdOrNull(mealId)
            ?: throw EntityNotFoundException("존재하지 않는 mealId 입니다.")

        meal.updateStatus(status)
    }

    @Transactional
    fun updateNutrition(mealId: Long) {

        val meal = mealRepository.findByIdOrNull(mealId)
            ?: throw EntityNotFoundException("존재하지 않는 mealId 입니다.")

        meal.updateNutrition(
            calory = 700,
            carbs = 100,
            protein = 50,
            fat = 20
        )
    }
}