package com.dandi.nyummy.meal.service

import com.dandi.nyummy.meal.dto.DailyNutritionEvaluation
import com.dandi.nyummy.meal.entity.Meal
import com.dandi.nyummy.meal.repository.MealRepository
import com.dandi.nyummy.profile.repository.ProfileRepository
import org.springframework.stereotype.Service

@Service
class MealService(
    val mealRepository: MealRepository,
    val profileRepository: ProfileRepository,
    val nutritionRecommendationCalculator: NutritionRecommendationCalculator
) {

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