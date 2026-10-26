package com.dandi.nyummy.room.dto

import com.dandi.nyummy.room.enum.RoomItemCategory
import java.time.Instant

/** 마이룸 아이템(상품) 목록 조회 결과 한 건. 이름·가격·생성 시각은 items에서 함께 읽는다. price가 null이면 비매품이다. */
data class RoomItemResult(
    val itemId: Long,
    val name: String,
    val price: Int?,
    val category: RoomItemCategory,
    val createdAt: Instant,
)
