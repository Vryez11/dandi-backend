package com.dandi.nyummy.room.service

import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.CommonErrorCode
import com.dandi.nyummy.exception.errorcode.ErrorCode
import com.dandi.nyummy.room.config.DefaultRoomItemProperties
import com.dandi.nyummy.room.config.RoomProperties
import com.dandi.nyummy.room.dto.EquippedRoomItemResult
import com.dandi.nyummy.room.dto.OwnedRoomItemResult
import com.dandi.nyummy.room.entity.OwnedRoomItem
import com.dandi.nyummy.room.enum.RoomItemCategory
import com.dandi.nyummy.room.repository.OwnedRoomItemRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomServiceTest {

    private val userId = 1L

    private val ownedRoomItemRepository = mockk<OwnedRoomItemRepository>()
    private val roomProperties = RoomProperties(
        defaultRoomItems = listOf(
            DefaultRoomItemProperties(itemId = 1L, x = 0f, y = 0f, zIndex = 0, scale = 1f),
            DefaultRoomItemProperties(itemId = 2L, x = 27f, y = 22f, zIndex = 2, scale = 0.8f),
        ),
    )

    private val roomService = RoomService(
        ownedRoomItemRepository = ownedRoomItemRepository,
        roomProperties = roomProperties,
    )

    private fun ownedRoomItem(id: Long, isEquipped: Boolean) = OwnedRoomItem(userId = userId, itemId = 2L).apply {
        this.isEquipped = isEquipped
        // id는 DB가 채우는 val이라 테스트에서만 직접 넣는다.
        ReflectionTestUtils.setField(this, "id", id)
    }

    private fun ownedRoomItemResult(ownedRoomItemId: Long, isEquipped: Boolean = false) =
        OwnedRoomItemResult(ownedRoomItemId = ownedRoomItemId, name = "호박 쿠션", itemId = 2L, isEquipped = isEquipped)

    private fun givenOwnedRoomItems(vararg results: OwnedRoomItemResult, hasNext: Boolean = false) {
        every {
            ownedRoomItemRepository.findOwnedRoomItemsByUserIdBeforeCursor(any(), any(), any(), any(), any())
        } returns SliceImpl(results.toList(), PageRequest.of(0, 10), hasNext)
    }

    private fun assertBusinessException(expected: ErrorCode, block: () -> Unit) {
        val exception = assertFailsWith<BusinessException>(block = block)
        assertEquals(expected, exception.errorCode)
    }

    // createDefaultOwnedRoomItems

    @Test
    fun `설정한 기본 아이템을 장착한 채로 설정한 좌표에 지급한다`() {
        val savedItems = slot<List<OwnedRoomItem>>()
        every { ownedRoomItemRepository.saveAll(capture(savedItems)) } answers { firstArg() }

        roomService.createDefaultOwnedRoomItems(userId)

        val items = savedItems.captured
        assertEquals(listOf(1L, 2L), items.map { it.itemId })
        assertTrue(items.all { it.userId == userId && it.isEquipped })
        assertEquals(27f, items[1].x)
        assertEquals(22f, items[1].y)
        assertEquals(2, items[1].zIndex)
        assertEquals(0.8f, items[1].scale)
    }

    // createOwnedRoomItems

    @Test
    fun `받은 아이템 id마다 보유 아이템을 장착하지 않은 채로 하나씩 만든다`() {
        val savedItems = slot<List<OwnedRoomItem>>()
        every { ownedRoomItemRepository.saveAll(capture(savedItems)) } answers { firstArg() }

        roomService.createOwnedRoomItems(userId, listOf(2L, 2L, 3L))

        val items = savedItems.captured
        assertEquals(listOf(2L, 2L, 3L), items.map { it.itemId })
        assertTrue(items.all { it.userId == userId && !it.isEquipped })
    }

    // getOwnedRoomItems

    @Test
    fun `첫 요청은 장착한 아이템의 가장 큰 id를 기준으로 조회해 전부를 대상으로 한다`() {
        givenOwnedRoomItems()

        roomService.getOwnedRoomItems(userId, RoomItemCategory.BED, cursor = null, size = 10)

        verify {
            ownedRoomItemRepository.findOwnedRoomItemsByUserIdBeforeCursor(
                userId,
                RoomItemCategory.BED,
                true,
                Long.MAX_VALUE,
                PageRequest.of(0, 10),
            )
        }
    }

    @Test
    fun `커서가 있으면 커서 아이템의 장착 여부와 id를 기준으로 조회한다`() {
        every { ownedRoomItemRepository.findOwnedRoomItemByIdAndUserId(30L, userId) } returns
            ownedRoomItem(id = 30L, isEquipped = false)
        givenOwnedRoomItems()

        roomService.getOwnedRoomItems(userId, RoomItemCategory.BED, cursor = 30L, size = 10)

        verify {
            ownedRoomItemRepository.findOwnedRoomItemsByUserIdBeforeCursor(
                userId,
                RoomItemCategory.BED,
                false,
                30L,
                PageRequest.of(0, 10),
            )
        }
    }

    @Test
    fun `커서 아이템이 없거나 다른 사용자의 것이면 목록을 조회하지 않고 거부한다`() {
        every { ownedRoomItemRepository.findOwnedRoomItemByIdAndUserId(30L, userId) } returns null

        assertBusinessException(CommonErrorCode.INVALID_INPUT_VALUE) {
            roomService.getOwnedRoomItems(userId, RoomItemCategory.BED, cursor = 30L, size = 10)
        }
        verify(exactly = 0) {
            ownedRoomItemRepository.findOwnedRoomItemsByUserIdBeforeCursor(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `다음 묶음이 있으면 마지막 보유 아이템 id를 nextCursor로 준다`() {
        givenOwnedRoomItems(ownedRoomItemResult(9, isEquipped = true), ownedRoomItemResult(7), hasNext = true)

        val result = roomService.getOwnedRoomItems(userId, RoomItemCategory.BED, cursor = null, size = 2)

        assertEquals(listOf(9L, 7L), result.ownedRoomItems.map { it.ownedRoomItemId })
        assertEquals(7L, result.nextCursor)
        assertTrue(result.hasNext)
    }

    @Test
    fun `마지막 묶음이면 nextCursor는 null이다`() {
        givenOwnedRoomItems(ownedRoomItemResult(3))

        val result = roomService.getOwnedRoomItems(userId, RoomItemCategory.BED, cursor = null, size = 2)

        assertNull(result.nextCursor)
        assertFalse(result.hasNext)
    }

    // getRoom

    @Test
    fun `보유 아이템이 없어도 모든 카테고리를 enum 순서대로 내려준다`() {
        givenOwnedRoomItems()
        every { ownedRoomItemRepository.findEquippedRoomItemsByUserId(userId) } returns emptyList()

        val room = roomService.getRoom(userId, size = 10)

        assertEquals(RoomItemCategory.entries, room.categories.map { it.category })
        assertEquals(RoomItemCategory.entries.map { it.description }, room.categories.map { it.categoryName })
        assertTrue(room.categories.all { it.ownedRoomItems.isEmpty() && !it.hasNext })
    }

    // getEquippedRoomItems

    @Test
    fun `장착한 아이템을 조회 순서 그대로 내려준다`() {
        every { ownedRoomItemRepository.findEquippedRoomItemsByUserId(userId) } returns listOf(
            EquippedRoomItemResult(5, RoomItemCategory.BACKGROUND, 1L, 0f, 0f, 0, 1f),
            EquippedRoomItemResult(6, RoomItemCategory.BED, 2L, 150f, 420f, 4, 1.2f),
        )

        val equippedRoomItems = roomService.getEquippedRoomItems(userId)

        assertEquals(listOf(5L, 6L), equippedRoomItems.map { it.ownedRoomItemId })
        assertEquals(2L, equippedRoomItems[1].itemId)
        assertEquals(1.2f, equippedRoomItems[1].scale)
    }
}
