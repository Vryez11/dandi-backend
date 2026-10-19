package com.dandi.nyummy.room.dto

import com.dandi.nyummy.room.enum.RoomItemCategory

/** 장착 아이템 조회 결과 한 건. 카테고리는 room_items에서 함께 읽는다. */
data class EquippedRoomItemResult(
    val roomItemId: Long,
    val category: RoomItemCategory,
    val itemId: Long,
    val x: Float,
    val y: Float,
    val zIndex: Int,
    val scale: Float,
)
