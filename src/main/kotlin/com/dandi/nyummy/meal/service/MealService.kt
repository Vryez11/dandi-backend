package com.dandi.nyummy.meal.service

import com.dandi.nyummy.meal.dto.DailyNutritionEvaluation
import com.dandi.nyummy.meal.dto.MonthlyMealDayResponse
import com.dandi.nyummy.meal.dto.MonthlyMealsResponse
import com.dandi.nyummy.meal.entity.Meal
import com.dandi.nyummy.meal.repository.MealRepository
import com.dandi.nyummy.profile.repository.ProfileRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

@Service
class MealService(
    val mealRepository: MealRepository,
    val profileRepository: ProfileRepository,
    val nutritionRecommendationCalculator: NutritionRecommendationCalculator
) {

    fun getMonthlyMeals(userId: Long, year: Int, month: Int): MonthlyMealsResponse {

        val range = MonthlyCalendarRange.calculate(YearMonth.of(year, month))

        val mealsByPeriod = mealRepository.getMealsByUserIdAndPeriod(
            userId,
            LocalDateTime.of(range.startDate, LocalTime.MIN),
            LocalDateTime.of(range.endDate, LocalTime.MAX)
        )

        val mealsByDate: Map<LocalDate, List<Meal>> =
            mealsByPeriod.filter { it.mealAt != null }
                .groupBy { it.mealAt!!.toLocalDate() }


        val profile = profileRepository.getProfileByUserId(userId)
        val recommended = nutritionRecommendationCalculator.calculateRecommendedDailyIntake(profile, LocalDate.now())

        val days = mutableListOf<MonthlyMealDayResponse>()
        var date = range.startDate
        while (date <= range.endDate) {

            days.add(
                MonthlyMealDayResponse(
                    date = date,
                    isCurrentMonth = date.year == year && date.monthValue == month,
                    dailyNutritionEvaluation = calculateDailyNutritionEvaluation(
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

    fun calculateDailyNutritionEvaluation(
        meals: List<Meal>,
        recommended: RecommendedDailyIntake,
    ): DailyNutritionEvaluation {

        if (meals.isEmpty()) {
            return DailyNutritionEvaluation.UNRECORDED
        }

        var totalCalories: Int = 0
        var totalCarbs: Int = 0
        var totalProteins: Int = 0
        var totalFats: Int = 0

        for (meal in meals) {
            totalCalories += meal.calory ?: 0
            totalCarbs += meal.carbs ?: 0
            totalProteins += meal.protein ?: 0
            totalFats += meal.fat ?: 0
        }

        val recommendedCalory = recommended.calory
        val recommendedCarbs = recommended.carbs
        val recommendedProtein = recommended.protein
        val recommendedFat = recommended.fat

        if (isPositiveNutrition(totalCalories, recommendedCalory) &&
            isPositiveNutrition(totalCarbs, recommendedCarbs) &&
            isPositiveNutrition(totalProteins, recommendedProtein) &&
            isPositiveNutrition(totalFats, recommendedFat)
        ) {

            return DailyNutritionEvaluation.POSITIVE
        }

        return DailyNutritionEvaluation.NEGATIVE
    }

    private fun isPositiveNutrition(totalValue: Int, recommendedValue: Int): Boolean {

        return 0.9 * recommendedValue <= totalValue && totalValue < 1.5 * recommendedValue
    }
}