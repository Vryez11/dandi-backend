package com.dandi.nyummy.meal.repository

import com.dandi.nyummy.meal.entity.Meal
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MealRepository : JpaRepository<Meal, Long>