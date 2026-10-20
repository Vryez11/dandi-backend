package com.dandi.nyummy.streak.entity

import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class StreakTest {

    private val zone: ZoneId = ZoneId.of("Asia/Seoul")

    // 2026-10-10 12:00 KST
    private val lastMealAt = Instant.parse("2026-10-10T03:00:00Z")

    private fun streak(streakDays: Int, lastMealAt: Instant = this.lastMealAt) =
        Streak(userId = 1L, lastMealAt = lastMealAt).apply { this.streakDays = streakDays }

    // updateStreakDays

    @Test
    fun `같은 날 다시 기록하면 일수는 그대로고 마지막 기록 시각만 바뀐다`() {
        val streak = streak(streakDays = 3)
        val mealAt = Instant.parse("2026-10-10T11:00:00Z") // 10/10 20:00 KST

        streak.updateStreakDays(mealAt, zone)

        assertEquals(3, streak.streakDays)
        assertEquals(mealAt, streak.lastMealAt)
    }

    @Test
    fun `다음 날 기록하면 일수가 하루 늘어난다`() {
        val streak = streak(streakDays = 3)
        val mealAt = Instant.parse("2026-10-11T03:00:00Z") // 10/11 12:00 KST

        streak.updateStreakDays(mealAt, zone)

        assertEquals(4, streak.streakDays)
        assertEquals(mealAt, streak.lastMealAt)
    }

    @Test
    fun `하루 이상 비우고 기록하면 1일부터 다시 센다`() {
        val streak = streak(streakDays = 3)
        val mealAt = Instant.parse("2026-10-12T03:00:00Z") // 10/12 12:00 KST

        streak.updateStreakDays(mealAt, zone)

        assertEquals(1, streak.streakDays)
        assertEquals(mealAt, streak.lastMealAt)
    }

    @Test
    fun `날짜는 UTC가 아니라 넘겨받은 시간대의 자정으로 나눈다`() {
        // UTC로는 둘 다 10월 10일이지만 KST로는 10/10 23:59와 10/11 00:00이다.
        val streak = streak(streakDays = 3, lastMealAt = Instant.parse("2026-10-10T14:59:00Z"))
        val mealAt = Instant.parse("2026-10-10T15:00:00Z")

        streak.updateStreakDays(mealAt, zone)

        assertEquals(4, streak.streakDays)
    }

    @Test
    fun `마지막 기록보다 이른 시각이 오면 아무것도 바꾸지 않는다`() {
        val streak = streak(streakDays = 3)
        val mealAt = Instant.parse("2026-10-09T03:00:00Z") // 10/9 12:00 KST

        streak.updateStreakDays(mealAt, zone)

        assertEquals(3, streak.streakDays)
        assertEquals(lastMealAt, streak.lastMealAt)
    }

    // calculateStreakDays

    @Test
    fun `마지막 기록일이 오늘이면 일수를 그대로 보여준다`() {
        val now = Instant.parse("2026-10-10T14:59:00Z") // 10/10 23:59 KST

        assertEquals(3, streak(streakDays = 3).calculateStreakDays(now, zone))
    }

    @Test
    fun `마지막 기록일이 어제면 오늘 아직 기록하지 않았어도 일수를 보여준다`() {
        val now = Instant.parse("2026-10-11T14:59:00Z") // 10/11 23:59 KST

        assertEquals(3, streak(streakDays = 3).calculateStreakDays(now, zone))
    }

    @Test
    fun `마지막 기록일이 그저께 이전이면 끊긴 것으로 보고 0을 보여준다`() {
        val now = Instant.parse("2026-10-11T15:00:00Z") // 10/12 00:00 KST

        assertEquals(0, streak(streakDays = 3).calculateStreakDays(now, zone))
    }
}
