package com.dandi.nyummy.meal.service

import com.dandi.nyummy.infra.aws.s3.S3Presigner
import com.dandi.nyummy.meal.dto.Nutrition
import com.dandi.nyummy.meal.dto.SingleMealResponse
import com.dandi.nyummy.meal.repository.MealRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

@Service
class SingleMealService(
    private val mealRepository: MealRepository,
    private val s3Presigner: S3Presigner
) {

    fun getSingleMeal(userId: Long, mealId: Long): SingleMealResponse {

        val meal = mealRepository.getMealByIdAndUserIdAndDeletedAtIsNull(mealId, userId)
            ?: throw Exception("Meal Not Found")

        val imageUrl = s3Presigner.getGetObjectUrl(meal.imageKey, 10.minutes)

        return SingleMealResponse(
            mealId = mealId,
            name = meal.name,
            mealAt = meal.mealAt,
            status = meal.status,
            nutrition = Nutrition(
                calory = meal.calory?:0,
                carbs = meal.carbs?:0,
                protein = meal.protein?:0,
                fat = meal.fat?:0,
            ),
            imageUrl = imageUrl
        )
    }

    @Transactional
    fun updateSingleMeal(userId: Long, mealId: Long, name: String): SingleMealResponse {

        val meal = mealRepository.getMealByIdAndUserIdAndDeletedAtIsNull(mealId, userId)
            ?: throw Exception("Meal Not Found")

        val imageUrl = s3Presigner.getGetObjectUrl(meal.imageKey, 10.minutes)

        meal.updateName(name)

        return SingleMealResponse(
            mealId = mealId,
            name = meal.name,
            mealAt = meal.mealAt,
            status = meal.status,
            nutrition = Nutrition(
                calory = meal.calory?:0,
                carbs = meal.carbs?: 0,
                protein = meal.protein?: 0,
                fat = meal.fat?: 0
            ),
            imageUrl = imageUrl
        )
    }

    @Transactional
    fun deleteSingleMeal(userId: Long, mealId: Long) {

        val meal = mealRepository.getMealByIdAndUserIdAndDeletedAtIsNull(mealId, userId)
            ?: throw Exception("Meal Not Found")

        meal.updateDeletedAt(Instant.now())
    }
}