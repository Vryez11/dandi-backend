@file:Suppress("ktlint:standard:filename")

package com.dandi.nyummy.room.dto

import com.dandi.nyummy.room.enum.RoomItemCategory
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive

data class RoomResponse(
    val categories: List<RoomItemCategoryResponse>,
    val equippedRoomItems: List<EquippedRoomItemResponse>,
)

data class RoomItemCategoryResponse(
    val category: RoomItemCategory,
    val categoryName: String,
    val ownedRoomItems: List<OwnedRoomItemResponse>,
    val nextCursor: Long?,
    val hasNext: Boolean,
)

data class OwnedRoomItemsResponse(
    val ownedRoomItems: List<OwnedRoomItemResponse>,
    val nextCursor: Long?,
    val hasNext: Boolean,
)

/**
 * @property itemId 상품 id. 앱은 `https://cdn.nyummy.co.kr/items/{itemId}.png`로 이미지를 조립한다
 */
data class OwnedRoomItemResponse(val ownedRoomItemId: Long, val name: String, val itemId: Long, val isEquipped: Boolean)

/**
 * @property itemId 상품 id. 앱은 `https://cdn.nyummy.co.kr/items/{itemId}.png`로 이미지를 조립한다
 */
data class EquippedRoomItemResponse(
    val ownedRoomItemId: Long,
    val category: RoomItemCategory,
    val itemId: Long,
    val x: Float,
    val y: Float,
    val zIndex: Int,
    val scale: Float,
)

data class UpdateEquippedRoomItemsRequest(
    @field:Valid
    val equippedRoomItems: List<EquippedRoomItemRequest>,
)

data class EquippedRoomItemRequest(
    val ownedRoomItemId: Long,

    @field:Min(value = 0, message = "x는 0 이상이어야 합니다.")
    @field:Max(value = 100, message = "x는 100 이하여야 합니다.")
    val x: Float,

    @field:Min(value = 0, message = "y는 0 이상이어야 합니다.")
    @field:Max(value = 100, message = "y는 100 이하여야 합니다.")
    val y: Float,

    val zIndex: Int,

    @field:Positive(message = "scale은 0보다 커야 합니다.")
    val scale: Float,
)
