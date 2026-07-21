package com.dandi.nyummy.meal.repository

import com.dandi.nyummy.meal.entity.Meal
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.data.jpa.repository.Query
import java.time.Instant

@Repository
interface MealRepository : JpaRepository<Meal, Long> {

    @Query(
        """
        select m from Meal as m
        where m.userId = :userId
        AND m.isDeleted = false
        AND m.mealAt >= :start 
        AND m.mealAt < :end
    """
    )
    fun getMealsByUserIdAndPeriod(userId: Long, start: Instant, end: Instant): List<Meal>

    fun getMealByIdAndUserIdAndIsDeletedIsFalse(mealId: Long, userId: Long): Meal?
}