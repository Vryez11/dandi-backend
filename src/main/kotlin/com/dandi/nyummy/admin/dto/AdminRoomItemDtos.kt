@file:Suppress("ktlint:standard:filename")

package com.dandi.nyummy.admin.dto

import com.dandi.nyummy.room.enum.RoomItemCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.Instant

/**
 * 이름과 가격을 항상 함께 보낸다(전체 교체). 키를 빼도 null로 받으므로 [price]를 빼면 비매품이 된다.
 *
 * @property price null이면 상점에서 팔지 않는 아이템(보상 전용)으로 바꾼다
 */
data class UpdateRoomItemRequest(
    @field:NotBlank(message = "아이템 이름은 필수입니다.")
    @field:Size(max = 100, message = "아이템 이름은 100자 이하여야 합니다.")
    val name: String,

    @field:PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
    val price: Int?,
)

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
