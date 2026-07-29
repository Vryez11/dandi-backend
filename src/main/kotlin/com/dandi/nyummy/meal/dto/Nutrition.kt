package com.dandi.nyummy.meal.dto

data class Nutrition(val calory: Int, val carbs: Int, val protein: Int, val fat: Int) {
    operator fun plus(other: Nutrition) = Nutrition(
        calory = calory + other.calory,
        carbs = carbs + other.carbs,
        protein = protein + other.protein,
        fat = fat + other.fat,
    )

    companion object {
        val ZERO = Nutrition(0, 0, 0, 0)
    }
}
