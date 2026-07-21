package com.dandi.nyummy.meal.controller

import com.dandi.nyummy.meal.dto.DailyMealsResponse
import com.dandi.nyummy.meal.dto.MonthlyMealsResponse
import com.dandi.nyummy.meal.dto.SingleMealResponse
import com.dandi.nyummy.meal.service.MealService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
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

    @GetMapping("/daily")
    fun getDailyMeals(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam day: Int,
    ) : DailyMealsResponse {

        return mealService.getDailyMeals(userId, year, month, day)
    }

    @GetMapping("/{mealId}")
    fun getSingleMeal(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable("mealId") mealId: Long,
    ) : SingleMealResponse {

        return mealService.getSingleMeal(userId, mealId)
    }

    @PutMapping("/{mealId}")
    fun updateSingleMeal(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable("mealId") mealId: Long,
        @RequestParam name: String,
    ) : SingleMealResponse {

        return mealService.updateSingleMeal(userId, mealId, name)
    }

    @DeleteMapping("/{mealId}")
    fun deleteSingleMeal(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable("mealId") mealId: Long,
        response: HttpServletResponse
    ) {

        mealService.deleteSingleMeal(userId, mealId)
        response.status = HttpStatus.NO_CONTENT.value()
    }
}