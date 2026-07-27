package com.dandi.nyummy.meal.service

import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.MealErrorCode
import com.dandi.nyummy.meal.dto.GetStatusResponse
import com.dandi.nyummy.meal.enum.MealStatus
import com.dandi.nyummy.meal.mapper.toGetStatusResponse
import com.dandi.nyummy.meal.repository.MealRepository
import jakarta.persistence.EntityNotFoundException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.seconds

@Service
class AnalysisService(private val mealRepository: MealRepository) {
    fun getStatus(mealId: Long): GetStatusResponse {
        val meal = mealRepository.findByIdOrNull(mealId)
            ?: throw BusinessException(MealErrorCode.MEAL_NOT_FOUND, "Meal Not Found")

        return meal.toGetStatusResponse()
    }

    fun analyzeNutrition(mealId: Long) {
        val meal = mealRepository.findByIdOrNull(mealId)

        if (meal == null) {
            println("check")
            throw BusinessException(MealErrorCode.MEAL_NOT_FOUND)
        }

        meal.updateStatus(MealStatus.ANALYZING)

        CoroutineScope(Dispatchers.IO).launch {
            println("영양소 분석 시작")
            delay(10.seconds)
            runCatching {
                meal.updateNutrition(
                    calory = 1000,
                    carbs = 1000,
                    protein = 1000,
                    fat = 1000,
                )
            }.onFailure {
                meal.updateStatus(MealStatus.FAILED)
            }
            println("영양소 분석 끝")
        }
    }

    fun retryNutritionAnalysis(mealId: Long): GetStatusResponse {
        val meal = mealRepository.findByIdOrNull(mealId)
            ?: throw EntityNotFoundException("존재하지 않는 mealId 입니다.")

        if (meal.status == MealStatus.FAILED) {
            meal.updateStatus(MealStatus.ANALYZING)
            analyzeNutrition(meal.id)
        }

        return meal.toGetStatusResponse()
    }
}
