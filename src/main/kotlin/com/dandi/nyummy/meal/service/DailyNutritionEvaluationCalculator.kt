package com.dandi.nyummy.meal.service

import com.dandi.nyummy.meal.dto.Nutrition
import com.dandi.nyummy.meal.entity.Meal
import com.dandi.nyummy.meal.enum.DailyNutritionEvaluation
import com.dandi.nyummy.meal.mapper.toNutrition

class DailyNutritionEvaluationCalculator {

    fun calculateDailyNutritionEvaluation(
        meals: List<Meal>,
        recommended: Nutrition,
    ): DailyNutritionEvaluation {

        if (meals.isEmpty()) {
            return DailyNutritionEvaluation.UNRECORDED
        }

        val totalNutrition = meals.fold(Nutrition.ZERO) {acc, meal -> acc.plus(meal.toNutrition())}

        if (isPositive(totalNutrition, recommended)) {
            return DailyNutritionEvaluation.POSITIVE
        }

        return DailyNutritionEvaluation.NEGATIVE
    }

    companion object {
        private val NUTRIENT = listOf(
            Nutrition::calory,
            Nutrition::carbs,
            Nutrition::protein,
            Nutrition::fat
        )
    }

    private fun isPositive(totalNutrition: Nutrition, recommended: Nutrition): Boolean  =
        NUTRIENT.all {nutrient -> isPositiveNutrition(nutrient(totalNutrition), nutrient(recommended))}

    fun isPositiveNutrition(totalValue: Int, recommendedValue: Int): Boolean {
        return recommendedValue * 0.9 <= totalValue && totalValue < recommendedValue * 1.5
    }
}