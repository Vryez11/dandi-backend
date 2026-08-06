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
import com.dandi.nyummy.security.AuthUser
import com.dandi.nyummy.security.CurrentUser
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/meals")
class MealController(private val mealService: MealService, private val analysisService: AnalysisService) {

    @GetMapping("/monthly")
    fun getMonthlyMeals(
        @CurrentUser user: AuthUser,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): MonthlyMealsResponse = mealService.getMonthlyMeals(user.userId, year, month)

    @GetMapping("/daily")
    fun getDailyMeals(
        @CurrentUser user: AuthUser,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam day: Int,
    ): DailyMealsResponse = mealService.getDailyMeals(user.userId, year, month, day)

    @GetMapping("/{mealId}")
    fun getMeal(@CurrentUser user: AuthUser, @PathVariable("mealId") mealId: Long): MealResponse =
        mealService.getMeal(user.userId, mealId)

    @PutMapping("/{mealId}")
    fun updateMeal(
        @CurrentUser user: AuthUser,
        @PathVariable("mealId") mealId: Long,
        @RequestParam name: String,
    ): MealResponse = mealService.updateMeal(user.userId, mealId, name)

    @DeleteMapping("/{mealId}")
    fun deleteMeal(@CurrentUser user: AuthUser, @PathVariable("mealId") mealId: Long, response: HttpServletResponse) {
        mealService.deleteMeal(user.userId, mealId)
        response.status = HttpStatus.NO_CONTENT.value()
    }

    @PostMapping
    fun createMeal(@CurrentUser user: AuthUser?, @Valid @RequestBody request: CreateMealRequest): GetStatusResponse =
        mealService.createMeal(user?.userId ?: 1L, request)

    @GetMapping("/{mealId}/analysis")
    fun getStatus(@CurrentUser user: AuthUser, @PathVariable @NotNull @Valid mealId: Long): GetStatusResponse =
        analysisService.getStatus(user.userId, mealId)

    @PostMapping("/{mealId}/analysis")
    fun retryAnalysis(@CurrentUser user: AuthUser, @PathVariable @NotNull @Valid mealId: Long): GetStatusResponse =
        analysisService.retryNutritionAnalysis(user.userId, mealId)

    @PostMapping("/images/presigned-url")
    fun getUploadUrl(@Valid @RequestBody request: UploadImageRequest): UploadImageResponse =
        mealService.createUploadUrl(request)
}
