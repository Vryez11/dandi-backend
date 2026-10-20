package com.dandi.nyummy.home.service

import com.dandi.nyummy.coin.service.CoinService
import com.dandi.nyummy.mail.service.MailService
import com.dandi.nyummy.meal.dto.TodayMealSummaryResponse
import com.dandi.nyummy.meal.service.MealService
import com.dandi.nyummy.room.service.RoomService
import com.dandi.nyummy.streak.dto.StreakResponse
import com.dandi.nyummy.streak.service.StreakService
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeServiceTest {

    private val userId = 1L

    private val mealService = mockk<MealService>()
    private val mailService = mockk<MailService>()
    private val roomService = mockk<RoomService>()
    private val coinService = mockk<CoinService>()
    private val streakService = mockk<StreakService>()

    private val homeService = HomeService(
        mealService = mealService,
        mailService = mailService,
        roomService = roomService,
        coinService = coinService,
        streakService = streakService,
    )

    init {
        every { coinService.getBalance(userId) } returns 0
        every { streakService.getStreak(userId) } returns StreakResponse(streakDays = 0, recordsUntilNextReward = 0)
        every { mealService.getTodayMealSummary(userId) } returns TodayMealSummaryResponse(
            todayRecordedCount = 0,
            todayAttemptCount = 0,
            todayMaxAttemptCount = 5,
            todayCurrentCalory = 0,
            todayTargetCalory = 2000,
        )
        every { roomService.getEquippedRoomItems(userId) } returns emptyList()
    }

    // getHome — streak

    @Test
    fun `연속 기록은 StreakService가 계산한 값을 그대로 담는다`() {
        every { mailService.existsNotClaimedMail(userId) } returns false
        every { streakService.getStreak(userId) } returns StreakResponse(streakDays = 3, recordsUntilNextReward = 0)

        assertEquals(StreakResponse(streakDays = 3, recordsUntilNextReward = 0), homeService.getHome(userId).streak)
    }

    // getHome — hasMailboxBadge

    @Test
    fun `받을 수 있는 우편이 있으면 우편함 배지를 켠다`() {
        every { mailService.existsNotClaimedMail(userId) } returns true

        assertTrue(homeService.getHome(userId).hasMailboxBadge)
    }

    @Test
    fun `받을 수 있는 우편이 없으면 우편함 배지를 끈다`() {
        every { mailService.existsNotClaimedMail(userId) } returns false

        assertFalse(homeService.getHome(userId).hasMailboxBadge)
    }
}
