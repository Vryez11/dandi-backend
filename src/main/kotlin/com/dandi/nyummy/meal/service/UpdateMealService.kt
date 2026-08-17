package com.dandi.nyummy.meal.service

import com.dandi.nyummy.meal.dto.Nutrition
import com.dandi.nyummy.meal.entity.Meal
import com.dandi.nyummy.meal.enum.MealStatus
import com.dandi.nyummy.meal.repository.MealRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateMealService(private val mealRepository: MealRepository) {

    @Transactional
    fun updateStatus(meal: Meal, status: MealStatus) {
        meal.updateStatus(status)
        mealRepository.save(meal)
    }

    @Transactional
    fun updateNutrition(meal: Meal, nutrition: Nutrition) {
        meal.updateNutrition(nutrition)
        mealRepository.save(meal)
    }
}
