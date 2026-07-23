package com.dandi.nyummy.meal.dto

import aws.smithy.kotlin.runtime.net.url.Url
import com.dandi.nyummy.meal.enum.MealStatus
import java.time.Instant

data class SingleMealResponse (

    val mealId: Long,

    val name: String,

    val mealAt: Instant,

    val status: MealStatus,

    val nutrition: Nutrition,

    val imageUrl: Url,
)