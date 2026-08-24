package com.dandi.nyummy.meal.enum

enum class MealStatus {

    WAITING,
    ANALYZING,
    COMPLETED,
    FAILED,
    UNKNOWN,
    ;

    companion object {
        val NON_ANALYZABLE_STATUSES = setOf(WAITING, ANALYZING, COMPLETED)
    }
}
