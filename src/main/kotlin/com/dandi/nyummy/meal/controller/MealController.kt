package com.dandi.nyummy.meal.controller

import com.dandi.nyummy.meal.dto.MonthlyMealsResponse
import com.dandi.nyummy.meal.service.MealService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/meals")
class MealController(private val mealService: MealService) {

    @GetMapping("/monthly")
    fun getMonthlyMeals(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): MonthlyMealsResponse {

        return mealService.getMonthlyMeals(userId, year, month)
    }
}