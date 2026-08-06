package com.dandi.nyummy.meal.service

import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.MealErrorCode
import com.dandi.nyummy.infra.ai.nutrition.NutritionAnalysisClient
import com.dandi.nyummy.meal.dto.GetStatusResponse
import com.dandi.nyummy.meal.enum.MealStatus
import com.dandi.nyummy.meal.mapper.toGetStatusResponse
import com.dandi.nyummy.meal.repository.MealRepository
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class AnalysisService(
    private val mealRepository: MealRepository,
    private val nutritionAnalysisClient: NutritionAnalysisClient,
) {
    fun getStatus(userId: Long, mealId: Long): GetStatusResponse {
        val meal = mealRepository.findByIdOrNull(mealId)
            ?: throw BusinessException(MealErrorCode.MEAL_NOT_FOUND, "Meal Not Found")

        meal.validateOwnership(userId)

        return meal.toGetStatusResponse()
    }

    @Transactional(TxType.REQUIRES_NEW)
    fun analyzeNutrition(userId: Long, mealId: Long) {
        val meal = mealRepository.findByIdOrNull(mealId)
            ?: throw BusinessException(MealErrorCode.MEAL_NOT_FOUND)

        // TODO: ANALYZING 방어 코드, 열어줘야 할 상태와 닫아야 할 상태 구분이 필요.

        meal.validateOwnership(userId)

        // TODO: 여기서 실패 한 번 해보기, 함수 업데이트는 다른 곳에서 매니징하는 느낌

        meal.updateStatus(MealStatus.ANALYZING)

        val nutrition = nutritionAnalysisClient.analyzeNutrition(meal.imageKey)

        meal.updateNutrition(nutrition)

        meal.updateStatus(MealStatus.COMPLETED)
    }

    fun retryNutritionAnalysis(userId: Long, mealId: Long): GetStatusResponse {
        val meal = mealRepository.findByIdOrNull(mealId)
            ?: throw BusinessException(MealErrorCode.MEAL_NOT_FOUND)

        meal.validateOwnership(userId)

        if (meal.status == MealStatus.FAILED) {
            meal.updateStatus(MealStatus.ANALYZING)
            analyzeNutrition(userId, meal.id)
        }

        return meal.toGetStatusResponse()
    }
}
