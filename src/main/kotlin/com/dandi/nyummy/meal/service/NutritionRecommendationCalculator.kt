package com.dandi.nyummy.meal.service

import com.dandi.nyummy.meal.dto.Nutrition
import com.dandi.nyummy.profile.entity.Profile
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period

class NutritionRecommendationCalculator {

    companion object {
        private val DEFAULT_INTAKE = Nutrition(
            calory = 2000,
            carbs = 250,
            protein = 100,
            fat = 67
        )
    }

    fun calculateRecommendedDailyIntake(profile: Profile?, today: LocalDate): Nutrition {

        val birth = profile?.birth ?: return DEFAULT_INTAKE
        val gender = profile.gender ?: return DEFAULT_INTAKE
        val height = profile.height ?: return DEFAULT_INTAKE
        val weight = profile.weight ?: return DEFAULT_INTAKE

        /**
         *  Mifflin-St Jeor 공식 (기초 대사량)
         *  : 실제 권장 칼로리는 기초 대사량 * 1.375 (활동량 계수)
         *  남성 : BMR = 10 * 몸무게 + 6.25 * 키 - 5 * 나이 + 5
         *  여성 : BMR = 10 * 몸무게 + 6.25 * 키 - 5 * 나이 - 161
         */

        val age = getAgeByBirth(birth, today)
        val bmr = (10 * weight + 6.25 * height - 5 * age + if (gender.toInt() == 0) 5 else -161)
        val calory = bmr * 1.375

        return Nutrition(
            calory = calory.toInt(),
            carbs = (calory * 0.5 / 4).toInt(),
            protein = (calory * 0.2 / 4).toInt(),
            fat = (calory * 0.3 / 9).toInt(),
        )
    }

    private fun getAgeByBirth(birth: LocalDateTime, today: LocalDate): Int {

        return Period.between(birth.toLocalDate(), today).years
    }
}

