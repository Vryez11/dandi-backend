@file:Suppress("ktlint:standard:filename")

package com.dandi.nyummy.admin.dto

import com.dandi.nyummy.room.enum.RoomItemCategory
import java.time.Instant

data class AdminRoomItemsResponse(val roomItems: List<AdminRoomItemResponse>)

/**
 * @property price null이면 상점에서 팔지 않는 아이템(보상 전용)
 * @property imageUrl 앱과 같은 규칙(CDN 주소 + `/items/{itemId}.png`)으로 만든 미리보기 URL
 */
data class AdminRoomItemResponse(
    val itemId: Long,
    val name: String,
    val price: Int?,
    val category: RoomItemCategory,
    val imageUrl: String,
    val createdAt: Instant,
)
