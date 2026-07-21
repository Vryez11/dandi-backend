package com.dandi.nyummy.meal.dto

import com.dandi.nyummy.meal.domain.Nutrition
import com.dandi.nyummy.meal.enum.MealStatus
import java.time.Instant
import java.time.LocalDate

data class DailyMealsResponse (

    val date: LocalDate,
    val meals: List<DailyMealResponse>,
    val dailyNutrition: DailyNutritionResponse
)

data class DailyMealResponse(

    val mealId: Long,
    val name: String,
    val mealAt: Instant,
    val calory: Int,
    val carbs: Int,
    val protein: Int,
    val fat: Int,
    val status: MealStatus
)

data class DailyNutritionResponse(

    val current: Nutrition,
    val target: Nutrition
)