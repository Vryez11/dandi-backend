package com.dandi.nyummy.meal.controller

import com.dandi.nyummy.meal.dto.CreateMealRequest
import com.dandi.nyummy.meal.dto.CreateMealResponse
import com.dandi.nyummy.meal.dto.UploadImageRequest
import com.dandi.nyummy.meal.dto.UploadImageResponse
import com.dandi.nyummy.meal.service.CreateMealService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/meals")
class CreateMealController(private val createMealService: CreateMealService) {

    @PostMapping("/images/presigned-url")
    suspend fun getUploadUrl(
        @Valid @RequestBody request: UploadImageRequest
    ): UploadImageResponse {
        return createMealService.getUploadUrl(request)
    }

    @PostMapping("/")
    suspend fun createMeal(
        @Valid @RequestBody request: CreateMealRequest
    ): CreateMealResponse {
        return createMealService.createMeal(request)
    }

}