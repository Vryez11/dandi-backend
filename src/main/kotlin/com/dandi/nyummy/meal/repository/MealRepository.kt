package com.dandi.nyummy.meal.repository

import com.dandi.nyummy.meal.entity.Meal
import java.time.Instant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MealRepository : JpaRepository<Meal, Long> {

    @Query(
        """
        select m from Meal as m
        where m.userId = :userId
        AND m.deletedAt is null
        AND m.mealAt >= :start
        AND m.mealAt < :end
    """,
    )
    fun getMealsByUserIdAndPeriod(userId: Long, start: Instant, end: Instant): List<Meal>

    fun getMealByIdAndUserIdAndDeletedAtIsNull(mealId: Long, userId: Long): Meal?
}
