package com.dandi.nyummy.meal.service

import com.dandi.nyummy.meal.enum.MealStatus
import com.dandi.nyummy.meal.repository.MealRepository
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class NutritionUpdateService(
    private val mealRepository: MealRepository,
) {

    @Transactional
    fun updateStatus(mealId: Long, status: MealStatus) {

        val meal = mealRepository.findByIdOrNull(mealId)
            ?: throw EntityNotFoundException("존재하지 않는 mealId 입니다.")

        meal.updateStatus(status)
    }

    @Transactional
    fun updateNutrition(mealId: Long) {

        val meal = mealRepository.findByIdOrNull(mealId)
            ?: throw EntityNotFoundException("존재하지 않는 mealId 입니다.")

        meal.updateNutrition(
            calory = 700,
            carbs = 100,
            protein = 50,
            fat = 20
        )
    }
}