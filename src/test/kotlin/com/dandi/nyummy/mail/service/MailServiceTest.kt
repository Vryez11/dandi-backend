package com.dandi.nyummy.mail.service

import com.dandi.nyummy.mail.entity.Mail
import com.dandi.nyummy.mail.enum.MailCategory
import com.dandi.nyummy.mail.enum.RewardBoxCategory
import com.dandi.nyummy.mail.repository.MailRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class MailServiceTest {

    private val userId = 1L
    private val expiresAt = Instant.parse("2026-10-25T00:00:00Z")

    private val mailRepository = mockk<MailRepository>()

    private val mailService = MailService(mailRepository = mailRepository)

    // createRewardBoxMail

    @Test
    fun `상자 우편은 REWARD_BOX로 만들고 등급 이름을 제목으로 쓴다`() {
        val savedMail = slot<Mail>()
        every { mailRepository.save(capture(savedMail)) } answers { firstArg() }

        mailService.createRewardBoxMail(
            userId = userId,
            rewardBoxCategory = RewardBoxCategory.GOLD,
            coinQuantity = 412,
            content = "식사 기록 보상",
            expiresAt = expiresAt,
        )

        assertEquals(userId, savedMail.captured.userId)
        assertEquals(MailCategory.REWARD_BOX, savedMail.captured.category)
        assertEquals(RewardBoxCategory.GOLD, savedMail.captured.rewardBoxCategory)
        assertEquals("골드 상자", savedMail.captured.title)
        assertEquals(412, savedMail.captured.coinQuantity)
        assertEquals("식사 기록 보상", savedMail.captured.content)
        assertEquals(expiresAt, savedMail.captured.expiresAt)
        assertNull(savedMail.captured.representativeItemId)
        assertFalse(savedMail.captured.isClaimed)
    }
}
