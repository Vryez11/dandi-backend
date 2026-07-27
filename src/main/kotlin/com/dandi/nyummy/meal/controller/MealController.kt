package com.dandi.nyummy.meal.controller

import com.dandi.nyummy.meal.dto.*
import com.dandi.nyummy.meal.service.AnalysisService
import com.dandi.nyummy.meal.service.MealService
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/meals")
class MealController(
    private val mealService: MealService,
    private val analysisService: AnalysisService,
) {

    @GetMapping("/monthly")
    fun getMonthlyMeals(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): MonthlyMealsResponse {

        return mealService.getMonthlyMeals(userId, year, month)
    }

    @GetMapping("/daily")
    fun getDailyMeals(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam day: Int,
    ): DailyMealsResponse {

        return mealService.getDailyMeals(userId, year, month, day)
    }

    @GetMapping("/{mealId}")
    fun getMeal(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable("mealId") mealId: Long,
    ): MealResponse {

        return mealService.getMeal(userId, mealId)
    }

    @PutMapping("/{mealId}")
    fun updateMeal(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable("mealId") mealId: Long,
        @RequestParam name: String,
    ): MealResponse {

        return mealService.updateMeal(userId, mealId, name)
    }

    @DeleteMapping("/{mealId}")
    fun deleteMeal(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable("mealId") mealId: Long,
        response: HttpServletResponse
    ) {

        mealService.deleteMeal(userId, mealId)
        response.status = HttpStatus.NO_CONTENT.value()
    }

    @PostMapping
    fun createMeal(
        @Valid @RequestBody request: CreateMealRequest
    ): GetStatusResponse {
        return mealService.createMeal(request)
    }

    @GetMapping("/{mealId}/analysis")
    fun getStatus(@PathVariable @NotNull @Valid mealId: Long): GetStatusResponse {
        return analysisService.getStatus(mealId)
    }

    @PostMapping("/{mealId}/analysis")
    fun retryNutritionAnalysis(@PathVariable @NotNull @Valid mealId: Long): GetStatusResponse {
        return analysisService.retryNutritionAnalysis(mealId)
    }

    @PostMapping("/images/presigned-url")
    fun createUploadUrl(
        @Valid @RequestBody request: UploadImageRequest
    ): UploadImageResponse {
        return mealService.createUploadUrl(request)
    }
}