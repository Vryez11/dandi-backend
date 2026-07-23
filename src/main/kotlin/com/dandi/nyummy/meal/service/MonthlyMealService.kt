package com.dandi.nyummy.meal.service

import com.dandi.nyummy.meal.dto.MonthlyMealDayResponse
import com.dandi.nyummy.meal.dto.MonthlyMealsResponse
import com.dandi.nyummy.meal.entity.Meal
import com.dandi.nyummy.meal.repository.MealRepository
import com.dandi.nyummy.profile.repository.ProfileRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@Service
class MonthlyMealService (
    private val mealRepository: MealRepository,
    private val profileRepository: ProfileRepository,
    private val nutritionRecommendationCalculator: NutritionRecommendationCalculator,
    private val dailyNutritionEvaluationCalculator: DailyNutritionEvaluationCalculator
) {

    fun getMonthlyMeals(userId: Long, year: Int, month: Int): MonthlyMealsResponse {

        val zone = ZoneId.of("Asia/Seoul")
        val range = MonthlyCalendarRange.calculate(YearMonth.of(year, month))

        val mealsByPeriod = mealRepository.getMealsByUserIdAndPeriod(
            userId,
            range.startDate.atStartOfDay(zone).toInstant(),
            range.endDate.plusDays(1).atStartOfDay(zone).toInstant()
        )


        val mealsByDate: Map<LocalDate, List<Meal>> =
            mealsByPeriod
                .groupBy { it.mealAt.atZone(zone).toLocalDate() }


        val profile = profileRepository.getProfileByUserId(userId)
        val recommended = nutritionRecommendationCalculator.calculateRecommendedDailyIntake(profile, LocalDate.now())

        val days = mutableListOf<MonthlyMealDayResponse>()
        var date = range.startDate
        while (date <= range.endDate) {

            days.add(
                MonthlyMealDayResponse(
                    date = date,
                    isCurrentMonth = date.year == year && date.monthValue == month,
                    dailyNutritionEvaluation = dailyNutritionEvaluationCalculator.calculateDailyNutritionEvaluation(
                        meals = mealsByDate[date] ?: emptyList(),
                        recommended = recommended
                    ),
                    foodIconIds = emptyList()
                )
            )
            date = date.plusDays(1)
        }

        return MonthlyMealsResponse(
            year = year,
            month = month,
            days = days
        )
    }
}