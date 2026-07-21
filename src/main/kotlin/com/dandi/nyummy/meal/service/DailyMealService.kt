package com.dandi.nyummy.meal.service

import com.dandi.nyummy.meal.domain.Nutrition
import com.dandi.nyummy.meal.domain.NutritionRecommendationCalculator
import com.dandi.nyummy.meal.dto.DailyMealResponse
import com.dandi.nyummy.meal.dto.DailyMealsResponse
import com.dandi.nyummy.meal.dto.DailyNutritionResponse
import com.dandi.nyummy.meal.repository.MealRepository
import com.dandi.nyummy.profile.repository.ProfileRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId

@Service
class DailyMealService (
    private val mealRepository: MealRepository,
    private val profileRepository: ProfileRepository,
    private val nutritionRecommendationCalculator: NutritionRecommendationCalculator
) {

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

        val recommended = nutritionRecommendationCalculator.calculateRecommendedDailyIntake(profile, LocalDate.of(year, month, day))

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
}