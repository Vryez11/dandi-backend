package com.dandi.nyummy.meal.controller

import com.dandi.nyummy.meal.dto.CreateMealRequest
import com.dandi.nyummy.meal.dto.GetStatusResponse
import com.dandi.nyummy.meal.dto.UploadImageRequest
import com.dandi.nyummy.meal.dto.UploadImageResponse
import com.dandi.nyummy.meal.service.CreateMealService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/meals")
class CreateMealController(private val createMealService: CreateMealService) {

    @PostMapping("/images/presigned-url")
    fun getUploadUrl(
        @Valid @RequestBody request: UploadImageRequest
    ): UploadImageResponse {
        return createMealService.createUploadUrl(request)
    }

    @PostMapping("/")
    fun createMeal(
        @Valid @RequestBody request: CreateMealRequest
    ): GetStatusResponse {
        return createMealService.createMeal(request)
    }

    @GetMapping("/{mealId}/analysis")
    fun getStatus(@PathVariable @NotNull @Valid mealId: Long): GetStatusResponse {
        return createMealService.getStatus(mealId)
    }

    @PostMapping("/{mealId}/analysis")
    fun retryAnalysis(@PathVariable @NotNull @Valid mealId: Long): GetStatusResponse {
        return createMealService.retryNutritionAnalysis(mealId)
    }

}