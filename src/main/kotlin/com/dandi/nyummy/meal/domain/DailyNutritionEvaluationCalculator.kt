package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.meal.entity.Meal
import com.dandi.nyummy.meal.enum.DailyNutritionEvaluation
import org.springframework.stereotype.Component

@Component
class DailyNutritionEvaluationCalculator {

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
                totalFat, recommendedFat)
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
}