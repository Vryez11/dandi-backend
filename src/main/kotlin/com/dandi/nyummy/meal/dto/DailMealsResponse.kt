package com.dandi.nyummy.meal.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class DailMealsResponse (

    val date: LocalDate,
    val meals: List<DailyMealResponse>,
    val dailyNutrition: DailyNutritionResponse
)

data class DailyMealResponse(

    val mealId: Long,
    val name: String,
    val mealAt: LocalDateTime,
    val calories: Int,
    val carbs: Int,
    val protein: Int,
    val fat: Int
)

data class DailyNutritionResponse(

    val current: Nutrition,
    val target: Nutrition
)