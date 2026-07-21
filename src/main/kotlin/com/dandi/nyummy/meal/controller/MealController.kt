package com.dandi.nyummy.meal.controller

import com.dandi.nyummy.meal.dto.CreateMealRequest
import com.dandi.nyummy.meal.dto.DailyMealsResponse
import com.dandi.nyummy.meal.dto.GetStatusResponse
import com.dandi.nyummy.meal.dto.MonthlyMealsResponse
import com.dandi.nyummy.meal.dto.SingleMealResponse
import com.dandi.nyummy.meal.dto.UploadImageRequest
import com.dandi.nyummy.meal.dto.UploadImageResponse
import com.dandi.nyummy.meal.service.AnalysisMealService
import com.dandi.nyummy.meal.service.DailyMealService
import com.dandi.nyummy.meal.service.MonthlyMealService
import com.dandi.nyummy.meal.service.SingleMealService
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/meals")
class MealController(
    private val monthlyMealService: MonthlyMealService,
    private val dailyMealService: DailyMealService,
    private val singleMealService: SingleMealService,
    private val analysisMealService: AnalysisMealService,
    ) {

    @GetMapping("/monthly")
    fun getMonthlyMeals(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): MonthlyMealsResponse {

        return monthlyMealService.getMonthlyMeals(userId, year, month)
    }

    @GetMapping("/daily")
    fun getDailyMeals(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam day: Int,
    ) : DailyMealsResponse {

        return dailyMealService.getDailyMeals(userId, year, month, day)
    }

    @GetMapping("/{mealId}")
    fun getSingleMeal(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable("mealId") mealId: Long,
    ) : SingleMealResponse {

        return singleMealService.getSingleMeal(userId, mealId)
    }

    @PutMapping("/{mealId}")
    fun updateSingleMeal(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable("mealId") mealId: Long,
        @RequestParam name: String,
    ) : SingleMealResponse {

        return singleMealService.updateSingleMeal(userId, mealId, name)
    }

    @DeleteMapping("/{mealId}")
    fun deleteSingleMeal(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable("mealId") mealId: Long,
        response: HttpServletResponse
    ) {

        singleMealService.deleteSingleMeal(userId, mealId)
        response.status = HttpStatus.NO_CONTENT.value()
    }

    @PostMapping
    fun createMeal(
        @Valid @RequestBody request: CreateMealRequest
    ): GetStatusResponse {
        return analysisMealService.createSingleMeal(request)
    }

    @GetMapping("/{mealId}/analysis")
    fun getStatus(@PathVariable @NotNull @Valid mealId: Long): GetStatusResponse {
        return analysisMealService.getStatus(mealId)
    }

    @PostMapping("/{mealId}/analysis")
    fun retryAnalysis(@PathVariable @NotNull @Valid mealId: Long): GetStatusResponse {
        return analysisMealService.retryNutritionAnalysis(mealId)
    }

    @PostMapping("/images/presigned-url")
    fun getUploadUrl(
        @Valid @RequestBody request: UploadImageRequest
    ): UploadImageResponse {
        return analysisMealService.createUploadUrl(request)
    }
}