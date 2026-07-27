package com.dandi.nyummy.meal.controller

import com.dandi.nyummy.meal.dto.CreateMealRequest
import com.dandi.nyummy.meal.dto.DailyMealsResponse
import com.dandi.nyummy.meal.dto.GetStatusResponse
import com.dandi.nyummy.meal.dto.MealResponse
import com.dandi.nyummy.meal.dto.MonthlyMealsResponse
import com.dandi.nyummy.meal.dto.UploadImageRequest
import com.dandi.nyummy.meal.dto.UploadImageResponse
import com.dandi.nyummy.meal.service.AnalysisService
import com.dandi.nyummy.meal.service.MealService
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/meals")
class MealController(private val mealService: MealService, private val analysisService: AnalysisService) {

    @GetMapping("/monthly")
    fun getMonthlyMeals(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): MonthlyMealsResponse = mealService.getMonthlyMeals(userId, year, month)

    @GetMapping("/daily")
    fun getDailyMeals(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam day: Int,
    ): DailyMealsResponse = mealService.getDailyMeals(userId, year, month, day)

    @GetMapping("/{mealId}")
    fun getMeal(@RequestHeader("X-User-Id") userId: Long, @PathVariable("mealId") mealId: Long): MealResponse =
        mealService.getMeal(userId, mealId)

    @PutMapping("/{mealId}")
    fun updateMeal(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable("mealId") mealId: Long,
        @RequestParam name: String,
    ): MealResponse = mealService.updateMeal(userId, mealId, name)

    @DeleteMapping("/{mealId}")
    fun deleteMeal(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable("mealId") mealId: Long,
        response: HttpServletResponse,
    ) {
        mealService.deleteMeal(userId, mealId)
        response.status = HttpStatus.NO_CONTENT.value()
    }

    @PostMapping
    fun createMeal(@Valid @RequestBody request: CreateMealRequest): GetStatusResponse = mealService.createMeal(request)

    @GetMapping("/{mealId}/analysis")
    fun getStatus(@PathVariable @NotNull @Valid mealId: Long): GetStatusResponse = analysisService.getStatus(mealId)

    @PostMapping("/{mealId}/analysis")
    fun retryAnalysis(@PathVariable @NotNull @Valid mealId: Long): GetStatusResponse =
        analysisService.retryNutritionAnalysis(mealId)

    @PostMapping("/images/presigned-url")
    fun getUploadUrl(@Valid @RequestBody request: UploadImageRequest): UploadImageResponse =
        mealService.createUploadUrl(request)
}
