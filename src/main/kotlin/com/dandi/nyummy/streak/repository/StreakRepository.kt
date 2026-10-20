package com.dandi.nyummy.streak.repository

import com.dandi.nyummy.streak.entity.Streak
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StreakRepository : JpaRepository<Streak, Long> {

    fun findByUserId(userId: Long): Streak?
}
