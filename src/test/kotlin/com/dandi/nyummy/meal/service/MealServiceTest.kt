package com.dandi.nyummy.meal.service

import com.dandi.nyummy.meal.dto.DailyNutritionEvaluation
import com.dandi.nyummy.meal.entity.Meal
import com.dandi.nyummy.meal.repository.MealRepository
import com.dandi.nyummy.profile.repository.ProfileRepository
import org.mockito.Mockito.mock
import kotlin.test.Test
import kotlin.test.assertEquals

class MealServiceTest {

    private val mealService = MealService(
        mealRepository = mock(MealRepository::class.java),
        profileRepository = mock(ProfileRepository::class.java),
        nutritionRecommendationCalculator = NutritionRecommendationCalculator(),
    )

    // 계산 편의를 위한 권장량: 90% / 150% 경계가 딱 떨어지는 값
    private val recommended = RecommendedDailyIntake(calory = 2000, carbs = 250, protein = 100, fat = 67)

    private fun meal(calory: Int?, carbs: Int?, protein: Int?, fat: Int?) =
        Meal(userId = 1, name = "식사", calory = calory, carbs = carbs, protein = protein, fat = fat)

    // 4개 지표 모두 90~150% 중간쯤에 들어가는 식사
    private fun mealInRange() = meal(calory = 2400, carbs = 280, protein = 110, fat = 75)

    @Test
    fun `식사 기록이 없으면 UNRECORDED를 반환한다`() {
        val result = mealService.calculateDailyNutritionEvaluation(emptyList(), recommended)

        assertEquals(DailyNutritionEvaluation.UNRECORDED, result)
    }

    @Test
    fun `모든 지표가 권장 범위 안이면 POSITIVE를 반환한다`() {
        val result = mealService.calculateDailyNutritionEvaluation(listOf(mealInRange()), recommended)

        assertEquals(DailyNutritionEvaluation.POSITIVE, result)
    }

    @Test
    fun `정확히 90퍼센트는 범위에 포함된다`() {
        // 칼로리 1800 = 2000의 90%, 나머지는 중간값
        val meals = listOf(meal(calory = 1800, carbs = 280, protein = 110, fat = 75))

        val result = mealService.calculateDailyNutritionEvaluation(meals, recommended)

        assertEquals(DailyNutritionEvaluation.POSITIVE, result)
    }

    @Test
    fun `정확히 150퍼센트는 범위에서 제외된다`() {
        // 칼로리 3000 = 2000의 150%
        val meals = listOf(meal(calory = 3000, carbs = 280, protein = 110, fat = 75))

        val result = mealService.calculateDailyNutritionEvaluation(meals, recommended)

        assertEquals(DailyNutritionEvaluation.NEGATIVE, result)
    }

    @Test
    fun `권장량에 못 미치면 NEGATIVE를 반환한다`() {
        // 칼로리 1500 = 2000의 75% (범위 미달)
        val meals = listOf(meal(calory = 1500, carbs = 280, protein = 110, fat = 75))

        val result = mealService.calculateDailyNutritionEvaluation(meals, recommended)

        assertEquals(DailyNutritionEvaluation.NEGATIVE, result)
    }

    @Test
    fun `한 지표만 벗어나도 NEGATIVE를 반환한다`() {
        // 단백질 200 = 100의 200%, 나머지는 전부 범위 안
        val meals = listOf(meal(calory = 2400, carbs = 280, protein = 200, fat = 75))

        val result = mealService.calculateDailyNutritionEvaluation(meals, recommended)

        assertEquals(DailyNutritionEvaluation.NEGATIVE, result)
    }

    @Test
    fun `여러 식사는 합산해서 판정한다`() {
        // 한 끼로는 미달이지만 세 끼 합치면 범위 안
        val meals = listOf(
            meal(calory = 800, carbs = 90, protein = 40, fat = 25),
            meal(calory = 800, carbs = 90, protein = 40, fat = 25),
            meal(calory = 800, carbs = 100, protein = 30, fat = 25),
        )

        val result = mealService.calculateDailyNutritionEvaluation(meals, recommended)

        assertEquals(DailyNutritionEvaluation.POSITIVE, result)
    }

    @Test
    fun `영양 정보가 전부 null인 기록은 UNRECORDED가 아니라 NEGATIVE다`() {
        // 기록 자체는 존재하므로 평가 대상 (null은 0으로 합산 → 전 지표 미달)
        val meals = listOf(meal(calory = null, carbs = null, protein = null, fat = null))

        val result = mealService.calculateDailyNutritionEvaluation(meals, recommended)

        assertEquals(DailyNutritionEvaluation.NEGATIVE, result)
    }
}
