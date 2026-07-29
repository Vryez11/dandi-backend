package com.dandi.nyummy.meal.service

import com.dandi.nyummy.infra.aws.s3.S3Presigner
import com.dandi.nyummy.meal.calculator.calculateDailyNutritionEvaluation
import com.dandi.nyummy.meal.calculator.calculateMonthlyCalendarRange
import com.dandi.nyummy.meal.calculator.calculateRecommendedDailyIntake
import com.dandi.nyummy.meal.dto.CreateMealRequest
import com.dandi.nyummy.meal.dto.DailyMealsResponse
import com.dandi.nyummy.meal.dto.DailyNutritionResponse
import com.dandi.nyummy.meal.dto.GetStatusResponse
import com.dandi.nyummy.meal.dto.MealResponse
import com.dandi.nyummy.meal.dto.MonthlyMealDayResponse
import com.dandi.nyummy.meal.dto.MonthlyMealsResponse
import com.dandi.nyummy.meal.dto.Nutrition
import com.dandi.nyummy.meal.dto.UploadImageRequest
import com.dandi.nyummy.meal.dto.UploadImageResponse
import com.dandi.nyummy.meal.entity.Meal
import com.dandi.nyummy.meal.enum.MealStatus
import com.dandi.nyummy.meal.mapper.toDailyMealResponse
import com.dandi.nyummy.meal.mapper.toEntity
import com.dandi.nyummy.meal.mapper.toGetStatusResponse
import com.dandi.nyummy.meal.mapper.toMealResponse
import com.dandi.nyummy.meal.mapper.toNutrition
import com.dandi.nyummy.meal.repository.MealRepository
import com.dandi.nyummy.profile.repository.ProfileRepository
import jakarta.transaction.Transactional
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
                duration = PRESIGNED_PUT_URL_EXPIRATION,
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
            expiresAt = expirationInstant.toString(),
        )
    }

    fun createMeal(request: CreateMealRequest): GetStatusResponse {
        val meal = request.toEntity()
        val savedMeal = mealRepository.save(meal)

        savedMeal.updateStatus(MealStatus.ANALYZING)

        analysisService.analyzeNutrition(savedMeal.id)

        return savedMeal.toGetStatusResponse()
    }

    fun getDailyMeals(userId: Long, year: Int, month: Int, day: Int): DailyMealsResponse {
        val zone = ZoneId.of("Asia/Seoul")
        val date = LocalDate.of(year, month, day)
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()

        val mealsByPeriod = mealRepository.getMealsByUserIdAndPeriod(userId, start, end)

        val meals = mealsByPeriod.map { it.toDailyMealResponse() }

        val profile = profileRepository.getProfileByUserId(userId)

        val recommended = calculateRecommendedDailyIntake(profile, LocalDate.of(year, month, day))

        val dailyNutrition = DailyNutritionResponse(
            current = mealsByPeriod.fold(Nutrition.ZERO) { acc, meal -> acc + meal.toNutrition() },
            target = recommended,
        )

        return DailyMealsResponse(
            date = LocalDate.of(year, month, day),
            meals = meals,
            dailyNutrition = dailyNutrition,
        )
    }

    fun getMonthlyMeals(userId: Long, year: Int, month: Int): MonthlyMealsResponse {
        val zone = ZoneId.of("Asia/Seoul")
        val (startDate, endDate) = calculateMonthlyCalendarRange(YearMonth.of(year, month))

        val mealsByPeriod = mealRepository.getMealsByUserIdAndPeriod(
            userId,
            startDate.atStartOfDay(zone).toInstant(),
            endDate.plusDays(1).atStartOfDay(zone).toInstant(),
        )

        val mealsByDate: Map<LocalDate, List<Meal>> =
            mealsByPeriod
                .groupBy { it.mealAt.atZone(zone).toLocalDate() }

        val profile = profileRepository.getProfileByUserId(userId)
        val recommended = calculateRecommendedDailyIntake(profile, LocalDate.now())

        val days = mutableListOf<MonthlyMealDayResponse>()
        var date = startDate
        while (date <= endDate) {
            days.add(
                MonthlyMealDayResponse(
                    date = date,
                    isCurrentMonth = date.year == year && date.monthValue == month,
                    dailyNutritionEvaluation = calculateDailyNutritionEvaluation(
                        meals = mealsByDate[date] ?: emptyList(),
                        recommended = recommended,
                    ),
                    foodIconIds = emptyList(),
                ),
            )
            date = date.plusDays(1)
        }

        return MonthlyMealsResponse(
            year = year,
            month = month,
            days = days,
        )
    }

    fun getMeal(userId: Long, mealId: Long): MealResponse {
        val meal = mealRepository.getMealByIdAndUserIdAndDeletedAtIsNull(mealId, userId)
            ?: throw Exception("Meal Not Found")

        val imageUrl = s3Presigner.getGetObjectUrl(meal.imageKey, 10.minutes).toString()

        return meal.toMealResponse(imageUrl)
    }

    @Transactional
    fun updateMeal(userId: Long, mealId: Long, name: String): MealResponse {
        val meal = mealRepository.getMealByIdAndUserIdAndDeletedAtIsNull(mealId, userId)
            ?: throw Exception("Meal Not Found")

        val imageUrl = s3Presigner.getGetObjectUrl(meal.imageKey, 10.minutes).toString()

        meal.updateName(name)

        return meal.toMealResponse(imageUrl)
    }

    @Transactional
    fun deleteMeal(userId: Long, mealId: Long) {
        val meal = mealRepository.getMealByIdAndUserIdAndDeletedAtIsNull(mealId, userId)
            ?: throw Exception("Meal Not Found")

        meal.updateDeletedAt(Instant.now())
    }
}
