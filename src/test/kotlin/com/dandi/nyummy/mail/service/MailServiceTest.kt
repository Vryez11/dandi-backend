package com.dandi.nyummy.mail.service

import com.dandi.nyummy.mail.entity.Mail
import com.dandi.nyummy.mail.enum.MailCategory
import com.dandi.nyummy.mail.enum.RewardBoxCategory
import com.dandi.nyummy.mail.repository.MailRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.test.util.ReflectionTestUtils
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MailServiceTest {

    private val now = Instant.parse("2026-10-19T03:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val userId = 1L
    private val expiresAt = Instant.parse("2026-10-25T00:00:00Z")

    private val mailRepository = mockk<MailRepository>()

    private val mailService = MailService(mailRepository = mailRepository, clock = clock)

    private fun mail(
        id: Long,
        category: MailCategory = MailCategory.COIN,
        representativeItemId: Long? = null,
        rewardBoxCategory: RewardBoxCategory? = null,
    ) = Mail(
        userId = userId,
        category = category,
        title = "제목",
        content = "식사 기록 보상",
        representativeItemId = representativeItemId,
        rewardBoxCategory = rewardBoxCategory,
        expiresAt = expiresAt,
    ).apply {
        // id는 DB가 채우는 val이라 테스트에서만 직접 넣는다.
        ReflectionTestUtils.setField(this, "id", id)
    }

    private fun givenMails(vararg mails: Mail) {
        every { mailRepository.findMailsByUserIdBeforeCursor(any(), any(), any(), any()) } returns
            SliceImpl(mails.toList(), PageRequest.of(0, 10), false)
    }

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

    // getMails

    @Test
    fun `첫 요청은 가장 큰 id와 현재 시각을 기준으로 size만큼 조회한다`() {
        givenMails()

        mailService.getMails(userId, cursor = null, size = 10)

        verify { mailRepository.findMailsByUserIdBeforeCursor(userId, Long.MAX_VALUE, now, PageRequest.of(0, 10)) }
    }

    @Test
    fun `다음 묶음이 있으면 마지막 우편 id를 nextCursor로 준다`() {
        every { mailRepository.findMailsByUserIdBeforeCursor(userId, 10L, now, PageRequest.of(0, 2)) } returns
            SliceImpl(listOf(mail(id = 9), mail(id = 7)), PageRequest.of(0, 2), true)

        val result = mailService.getMails(userId, cursor = 10L, size = 2)

        assertEquals(listOf(9L, 7L), result.mails.map { it.mailId })
        assertEquals(7L, result.nextCursor)
        assertTrue(result.hasNext)
    }

    @Test
    fun `마지막 묶음이면 nextCursor는 null이다`() {
        every { mailRepository.findMailsByUserIdBeforeCursor(userId, 10L, now, PageRequest.of(0, 2)) } returns
            SliceImpl(listOf(mail(id = 3)), PageRequest.of(0, 2), false)

        val result = mailService.getMails(userId, cursor = 10L, size = 2)

        assertNull(result.nextCursor)
        assertFalse(result.hasNext)
    }

    @Test
    fun `상자 우편은 imageId로 등급 번호를 준다`() {
        givenMails(mail(id = 1, category = MailCategory.REWARD_BOX, rewardBoxCategory = RewardBoxCategory.SILVER))

        val mail = mailService.getMails(userId, cursor = null, size = 10).mails.single()

        assertEquals(2L, mail.imageId)
    }

    @Test
    fun `아이템 우편은 imageId로 대표 아이템 id를 준다`() {
        givenMails(mail(id = 1, category = MailCategory.ITEM, representativeItemId = 3L))

        val mail = mailService.getMails(userId, cursor = null, size = 10).mails.single()

        assertEquals(3L, mail.imageId)
    }

    @Test
    fun `코인 우편은 imageId로 코인 이미지 번호 1을 준다`() {
        givenMails(mail(id = 1, category = MailCategory.COIN))

        val mail = mailService.getMails(userId, cursor = null, size = 10).mails.single()

        assertEquals(1L, mail.imageId)
    }
}
