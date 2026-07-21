package com.dandi.nyummy.meal.dto

import com.dandi.nyummy.meal.enum.DailyNutritionEvaluation
import java.time.LocalDate

data class MonthlyMealsResponse(

    val year: Int,
    val month: Int,
    val days: List<MonthlyMealDayResponse>
)

data class MonthlyMealDayResponse(

    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val dailyNutritionEvaluation: DailyNutritionEvaluation,
    val foodIconIds: List<Long>
)