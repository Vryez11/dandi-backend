package com.dandi.nyummy.mail.service

import com.dandi.nyummy.coin.enum.CoinHistoryCategory
import com.dandi.nyummy.coin.service.CoinService
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.AuthErrorCode
import com.dandi.nyummy.exception.errorcode.ErrorCode
import com.dandi.nyummy.exception.errorcode.MailErrorCode
import com.dandi.nyummy.mail.dto.MailItemResult
import com.dandi.nyummy.mail.entity.Mail
import com.dandi.nyummy.mail.enum.MailCategory
import com.dandi.nyummy.mail.enum.RewardBoxCategory
import com.dandi.nyummy.mail.repository.MailItemRepository
import com.dandi.nyummy.mail.repository.MailRepository
import com.dandi.nyummy.room.service.RoomService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MailServiceTest {

    private val now = Instant.parse("2026-10-19T03:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val userId = 1L
    private val expiresAt = Instant.parse("2026-10-25T00:00:00Z")

    private val mailId = 10L

    private val mailRepository = mockk<MailRepository>()
    private val mailItemRepository = mockk<MailItemRepository>()
    private val coinService = mockk<CoinService>()
    private val roomService = mockk<RoomService>()

    private val mailService = MailService(
        mailRepository = mailRepository,
        mailItemRepository = mailItemRepository,
        coinService = coinService,
        roomService = roomService,
        clock = clock,
    )

    init {
        every { coinService.increaseBalance(any(), any(), any(), any()) } returns 0
        every { roomService.createOwnedRoomItems(any(), any()) } just runs
        every { mailItemRepository.findMailItemsByMailIds(any()) } returns emptyList()
    }

    private fun mail(
        id: Long,
        category: MailCategory = MailCategory.COIN,
        ownerId: Long = userId,
        coinQuantity: Int? = null,
        representativeItemId: Long? = null,
        rewardBoxCategory: RewardBoxCategory? = null,
    ) = Mail(
        userId = ownerId,
        category = category,
        title = "제목",
        content = "식사 기록 보상",
        coinQuantity = coinQuantity,
        representativeItemId = representativeItemId,
        rewardBoxCategory = rewardBoxCategory,
        expiresAt = expiresAt,
    ).apply {
        // id는 DB가 채우는 val이라 테스트에서만 직접 넣는다.
        ReflectionTestUtils.setField(this, "id", id)
    }

    private fun givenMail(mail: Mail?) {
        every { mailRepository.findMailWithLockByIdAndDeletedAtIsNull(mailId) } returns mail
    }

    private fun givenMailItems(vararg mailItems: MailItemResult) {
        every { mailItemRepository.findMailItemsByMailIds(listOf(mailId)) } returns mailItems.toList()
    }

    private fun mailItem(itemId: Long, name: String, quantity: Int) =
        MailItemResult(mailId = mailId, itemId = itemId, name = name, quantity = quantity)

    private fun assertBusinessException(expected: ErrorCode, block: () -> Unit) {
        val exception = assertFailsWith<BusinessException>(block = block)
        assertEquals(expected, exception.errorCode)
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

    // claimMail

    @Test
    fun `상자 우편을 받으면 상자에 든 코인을 지급하고 코인과 상자 번호를 준다`() {
        val mail = mail(
            id = mailId,
            category = MailCategory.REWARD_BOX,
            coinQuantity = 270,
            rewardBoxCategory = RewardBoxCategory.SILVER,
        )
        givenMail(mail)

        val reward = mailService.claimMail(userId, mailId)

        verify { coinService.increaseBalance(userId, 270, CoinHistoryCategory.MAIL_CLAIM, mailId) }
        assertEquals(now, mail.claimedAt)
        assertEquals(270, reward.coin?.quantity)
        assertEquals(1L, reward.coin?.imageId)
        assertEquals(2L, reward.rewardBox?.imageId)
        assertNull(reward.rewardBox?.animationUrl)
        assertNull(reward.items)
    }

    @Test
    fun `코인 우편을 받으면 코인을 지급하고 상자는 null이다`() {
        givenMail(mail(id = mailId, category = MailCategory.COIN, coinQuantity = 100))

        val reward = mailService.claimMail(userId, mailId)

        verify { coinService.increaseBalance(userId, 100, CoinHistoryCategory.MAIL_CLAIM, mailId) }
        assertEquals(100, reward.coin?.quantity)
        assertNull(reward.rewardBox)
    }

    @Test
    fun `코인이 없는 우편은 코인을 지급하지 않는다`() {
        givenMail(mail(id = mailId, category = MailCategory.ITEM, representativeItemId = 3L))

        val reward = mailService.claimMail(userId, mailId)

        verify(exactly = 0) { coinService.increaseBalance(any(), any(), any(), any()) }
        assertNull(reward.coin)
    }

    @Test
    fun `아이템 우편을 받으면 개수만큼 보유 아이템을 만들고 아이템을 준다`() {
        givenMail(mail(id = mailId, category = MailCategory.ITEM, representativeItemId = 2L))
        givenMailItems(mailItem(itemId = 2L, name = "호박 쿠션", quantity = 2))

        val reward = mailService.claimMail(userId, mailId)

        verify { roomService.createOwnedRoomItems(userId, listOf(2L, 2L)) }
        val item = reward.items?.single()
        assertEquals(2L, item?.imageId)
        assertEquals("호박 쿠션", item?.name)
        assertEquals(2, item?.quantity)
    }

    @Test
    fun `상자에 든 아이템도 지급하고 코인·상자와 함께 준다`() {
        givenMail(
            mail(
                id = mailId,
                category = MailCategory.REWARD_BOX,
                coinQuantity = 30,
                rewardBoxCategory = RewardBoxCategory.BRONZE,
            ),
        )
        givenMailItems(mailItem(itemId = 3L, name = "스크래처 타워", quantity = 1))

        val reward = mailService.claimMail(userId, mailId)

        verify { roomService.createOwnedRoomItems(userId, listOf(3L)) }
        verify { coinService.increaseBalance(userId, 30, CoinHistoryCategory.MAIL_CLAIM, mailId) }
        assertEquals(listOf(3L), reward.items?.map { it.imageId })
        assertEquals(30, reward.coin?.quantity)
        assertEquals(1L, reward.rewardBox?.imageId)
    }

    @Test
    fun `같은 아이템이 여러 줄이면 하나로 합치고 처음 넣은 순서대로 준다`() {
        givenMail(mail(id = mailId, category = MailCategory.ITEM, representativeItemId = 3L))
        givenMailItems(
            mailItem(itemId = 3L, name = "스크래처 타워", quantity = 1),
            mailItem(itemId = 2L, name = "호박 쿠션", quantity = 1),
            mailItem(itemId = 3L, name = "스크래처 타워", quantity = 2),
        )

        val reward = mailService.claimMail(userId, mailId)

        verify { roomService.createOwnedRoomItems(userId, listOf(3L, 2L, 3L, 3L)) }
        assertEquals(listOf(3L to 3, 2L to 1), reward.items?.map { it.imageId to it.quantity })
    }

    @Test
    fun `아이템이 없는 우편은 보유 아이템을 만들지 않고 items는 null이다`() {
        givenMail(mail(id = mailId, category = MailCategory.COIN, coinQuantity = 100))

        val reward = mailService.claimMail(userId, mailId)

        verify(exactly = 0) { roomService.createOwnedRoomItems(any(), any()) }
        assertNull(reward.items)
    }

    @Test
    fun `없거나 삭제된 우편이면 거부한다`() {
        givenMail(null)

        assertBusinessException(MailErrorCode.MAIL_NOT_FOUND) { mailService.claimMail(userId, mailId) }
        verify(exactly = 0) { coinService.increaseBalance(any(), any(), any(), any()) }
    }

    @Test
    fun `다른 사용자의 우편이면 받은 상태로 바꾸지 않고 거부한다`() {
        val mail = mail(id = mailId, ownerId = 2L, coinQuantity = 100)
        givenMail(mail)

        assertBusinessException(AuthErrorCode.FORBIDDEN) { mailService.claimMail(userId, mailId) }
        assertNull(mail.claimedAt)
        verify(exactly = 0) { coinService.increaseBalance(any(), any(), any(), any()) }
        verify(exactly = 0) { roomService.createOwnedRoomItems(any(), any()) }
    }

    @Test
    fun `이미 받은 우편이면 코인을 다시 지급하지 않는다`() {
        givenMail(mail(id = mailId, coinQuantity = 100).apply { claimedAt = now.minusSeconds(600) })

        assertBusinessException(MailErrorCode.ALREADY_CLAIMED) { mailService.claimMail(userId, mailId) }
        verify(exactly = 0) { coinService.increaseBalance(any(), any(), any(), any()) }
        verify(exactly = 0) { roomService.createOwnedRoomItems(any(), any()) }
    }
}
