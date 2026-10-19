package com.dandi.nyummy.room.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OwnedRoomItemTest {

    private fun ownedRoomItem(isEquipped: Boolean = false) = OwnedRoomItem(userId = 1L, itemId = 2L).apply {
        this.isEquipped = isEquipped
        x = 27f
        y = 22f
        zIndex = 2
        scale = 0.8f
    }

    // equip

    @Test
    fun `장착하면 위치와 크기를 요청한 값으로 바꾼다`() {
        val item = ownedRoomItem()

        item.equip(x = 40f, y = 60f, zIndex = 5, scale = 1.2f)

        assertTrue(item.isEquipped)
        assertEquals(40f, item.x)
        assertEquals(60f, item.y)
        assertEquals(5, item.zIndex)
        assertEquals(1.2f, item.scale)
    }

    @Test
    fun `이미 장착한 아이템을 다시 장착하면 장착한 채로 위치만 바뀐다`() {
        val item = ownedRoomItem(isEquipped = true)

        item.equip(x = 0f, y = 100f, zIndex = 0, scale = 1f)

        assertTrue(item.isEquipped)
        assertEquals(0f, item.x)
        assertEquals(100f, item.y)
    }

    // unequip

    @Test
    fun `장착을 해제해도 마지막 위치와 크기는 남는다`() {
        val item = ownedRoomItem(isEquipped = true)

        item.unequip()

        assertFalse(item.isEquipped)
        assertEquals(27f, item.x)
        assertEquals(22f, item.y)
        assertEquals(2, item.zIndex)
        assertEquals(0.8f, item.scale)
    }
}
