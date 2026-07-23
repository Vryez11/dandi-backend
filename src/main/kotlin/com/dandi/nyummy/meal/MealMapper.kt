package com.dandi.nyummy.meal

import com.dandi.nyummy.meal.dto.CreateMealRequest
import com.dandi.nyummy.meal.dto.GetStatusResponse
import com.dandi.nyummy.meal.dto.Nutrition
import com.dandi.nyummy.meal.entity.Meal
import com.dandi.nyummy.meal.enum.MealStatus
import java.time.Instant

// DTO -> Entity 변환 확장 함수
fun CreateMealRequest.toEntity() = Meal(
    name = this.name,
    status = MealStatus.ANALYSIS,
    imageKey = this.imageKey,
    mealAt = this.mealAt,
    createdAt = Instant.now(),
    userId = 1L,
    iconId = 1L
)

fun Meal.toGetStatusResponse() = GetStatusResponse(
    id = this.id,
    status = this.status.name,
    nutrition = Nutrition(
        calory = this.calory ?: 0,
        carbs = this.carbs ?: 0,
        protein = this.protein ?: 0,
        fat = this.fat ?: 0
    )
)