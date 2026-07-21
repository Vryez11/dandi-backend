package com.dandi.nyummy.meal.dto

import java.time.LocalDateTime

data class SingleMealResponse (

    val mealId: Long,

    val name: String,

    val mealAt: LocalDateTime,

    val status : MealStatus,

    val nutrition: Nutrition,

    val imageUrl: String,
)