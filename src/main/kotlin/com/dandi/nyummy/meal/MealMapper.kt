package com.dandi.nyummy.meal

import com.dandi.nyummy.common.enum.Status
import com.dandi.nyummy.meal.dto.CreateMealRequest
import com.dandi.nyummy.meal.dto.CreateMealResponse
import com.dandi.nyummy.meal.entity.Meal
import java.time.Instant

// Create Meal
// Entity -> DTO 변환 확장 함수
fun Meal.toResponse() = CreateMealResponse(
    id = this.id,
    status = this.status,
)

// DTO -> Entity 변환 확장 함수
fun CreateMealRequest.toEntity() = Meal(
    name = this.name,
    status = Status.ANALYSIS.toString(),
    imageKey = this.imageKey,
    mealAt = this.mealAt,
    createdAt = Instant.now(),
    userId = 1L,
    iconId = 1L,
)

// DTO -> Entity
//fun Meal.updateNutrition(dto: MealNutritionUpdateRequest) {
//    this.carbs = dto.carbs
//    this.protein = dto.protein
//}