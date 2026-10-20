package com.dandi.nyummy.streak.service

import com.dandi.nyummy.streak.dto.StreakResponse
import com.dandi.nyummy.streak.entity.Streak
import com.dandi.nyummy.streak.repository.StreakRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class StreakServiceTest {

    // 2026-10-11 00:30 KST. UTC로는 아직 10월 10일이라, 서비스가 Clock의 zone이 아니라
    // 식사 기준 타임존(Asia/Seoul)으로 날짜를 나누는지 드러난다.
    private val now = Instant.parse("2026-10-10T15:30:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val userId = 1L

    private val yesterday = Instant.parse("2026-10-10T03:00:00Z") // 10/10 12:00 KST
    private val twoDaysAgo = Instant.parse("2026-10-09T03:00:00Z") // 10/9 12:00 KST

    private val streakRepository = mockk<StreakRepository>()

    private val streakService = StreakService(
        streakRepository = streakRepository,
        clock = clock,
    )

    private fun streak(streakDays: Int, lastMealAt: Instant) =
        Streak(userId = userId, lastMealAt = lastMealAt).apply { this.streakDays = streakDays }

    // updateStreak

    @Test
    fun `첫 기록이면 지금 시각을 마지막 기록으로 1일짜리 행을 만든다`() {
        val savedStreak = slot<Streak>()
        every { streakRepository.findByUserId(userId) } returns null
        every { streakRepository.save(capture(savedStreak)) } answers { firstArg() }

        streakService.updateStreak(userId)

        assertEquals(userId, savedStreak.captured.userId)
        assertEquals(1, savedStreak.captured.streakDays)
        assertEquals(now, savedStreak.captured.lastMealAt)
    }

    @Test
    fun `행이 있으면 지금 시각으로 일수를 갱신하고 새 행을 만들지 않는다`() {
        val streak = streak(streakDays = 3, lastMealAt = yesterday)
        every { streakRepository.findByUserId(userId) } returns streak

        streakService.updateStreak(userId)

        assertEquals(4, streak.streakDays)
        assertEquals(now, streak.lastMealAt)
        verify(exactly = 0) { streakRepository.save(any()) }
    }

    // getStreak

    @Test
    fun `행이 없으면 0일을 반환한다`() {
        every { streakRepository.findByUserId(userId) } returns null

        assertEquals(StreakResponse(streakDays = 0, recordsUntilNextReward = 0), streakService.getStreak(userId))
    }

    @Test
    fun `어제까지 이어졌으면 일수를 반환한다`() {
        every { streakRepository.findByUserId(userId) } returns streak(streakDays = 3, lastMealAt = yesterday)

        assertEquals(StreakResponse(streakDays = 3, recordsUntilNextReward = 0), streakService.getStreak(userId))
    }

    @Test
    fun `그저께 이전에 끊겼으면 0일을 반환한다`() {
        every { streakRepository.findByUserId(userId) } returns streak(streakDays = 3, lastMealAt = twoDaysAgo)

        assertEquals(StreakResponse(streakDays = 0, recordsUntilNextReward = 0), streakService.getStreak(userId))
    }
}
