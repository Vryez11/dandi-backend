package com.dandi.nyummy.meal.service

import com.dandi.nyummy.profile.entity.Profile
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class NutritionRecommendationCalculatorTest {

    private val calculator = NutritionRecommendationCalculator()
    private val today = LocalDate.of(2026, 7, 16)

    private val defaultIntake = RecommendedDailyIntake(calory = 2000, carbs = 250, protein = 100, fat = 67)

    @Test
    fun `프로필이 null이면 기본 권장량을 반환한다`() {
        val result = calculator.calculateRecommendedDailyIntake(null, today)

        assertEquals(defaultIntake, result)
    }

    @Test
    fun `키가 없으면 기본 권장량을 반환한다`() {
        val profile = Profile(
            birth = LocalDateTime.of(2001, 3, 15, 0, 0),
            gender = 0,
            height = null,
            weight = 70,
        )

        val result = calculator.calculateRecommendedDailyIntake(profile, today)

        assertEquals(defaultIntake, result)
    }

    @Test
    fun `성별이 없으면 기본 권장량을 반환한다`() {
        val profile = Profile(
            birth = LocalDateTime.of(2001, 3, 15, 0, 0),
            gender = null,
            height = 175,
            weight = 70,
        )

        val result = calculator.calculateRecommendedDailyIntake(profile, today)

        assertEquals(defaultIntake, result)
    }

    @Test
    fun `남성 프로필의 권장량을 계산한다`() {
        // 만 25세, 175cm, 70kg 남성
        // BMR = 10*70 + 6.25*175 - 5*25 + 5 = 1673.75
        // 칼로리 = 1673.75 * 1.375 = 2301.40625
        val profile = Profile(
            birth = LocalDateTime.of(2001, 3, 15, 0, 0),
            gender = 0,
            height = 175,
            weight = 70,
        )

        val result = calculator.calculateRecommendedDailyIntake(profile, today)

        assertEquals(2301, result.calory)
        assertEquals(287, result.carbs)    // 2301.40625 * 0.5 / 4 = 287.6...
        assertEquals(115, result.protein)  // 2301.40625 * 0.2 / 4 = 115.0...
        assertEquals(76, result.fat)       // 2301.40625 * 0.3 / 9 = 76.7...
    }

    @Test
    fun `여성 프로필의 권장량을 계산한다`() {
        // 만 25세, 175cm, 70kg 여성
        // BMR = 10*70 + 6.25*175 - 5*25 - 161 = 1507.75
        // 칼로리 = 1507.75 * 1.375 = 2073.15625
        val profile = Profile(
            birth = LocalDateTime.of(2001, 3, 15, 0, 0),
            gender = 1,
            height = 175,
            weight = 70,
        )

        val result = calculator.calculateRecommendedDailyIntake(profile, today)

        assertEquals(2073, result.calory)
        assertEquals(259, result.carbs)
        assertEquals(103, result.protein)
        assertEquals(69, result.fat)
    }

    @Test
    fun `생일이 지나지 않았으면 만 나이로 계산한다`() {
        // 2001-12-01생은 2026-07-16 기준 만 24세 (연도 빼기로는 25)
        // BMR = 10*70 + 6.25*175 - 5*24 + 5 = 1678.75
        // 칼로리 = 1678.75 * 1.375 = 2308.28125
        val profile = Profile(
            birth = LocalDateTime.of(2001, 12, 1, 0, 0),
            gender = 0,
            height = 175,
            weight = 70,
        )

        val result = calculator.calculateRecommendedDailyIntake(profile, today)

        assertEquals(2308, result.calory)
    }
}
