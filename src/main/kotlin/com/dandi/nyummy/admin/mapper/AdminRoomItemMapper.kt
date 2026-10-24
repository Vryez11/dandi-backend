package com.dandi.nyummy.admin.mapper

import com.dandi.nyummy.admin.dto.AdminRoomItemResponse
import com.dandi.nyummy.room.dto.RoomItemResult

fun RoomItemResult.toAdminRoomItemResponse(imageUrl: String): AdminRoomItemResponse = AdminRoomItemResponse(
    itemId = this.itemId,
    name = this.name,
    price = this.price,
    category = this.category,
    imageUrl = imageUrl,
    createdAt = this.createdAt,
)
