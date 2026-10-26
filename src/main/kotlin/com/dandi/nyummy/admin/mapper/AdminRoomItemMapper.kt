package com.dandi.nyummy.admin.mapper

import com.dandi.nyummy.admin.dto.AdminRoomItemResponse
import com.dandi.nyummy.item.entity.Item
import com.dandi.nyummy.room.dto.RoomItemResult
import com.dandi.nyummy.room.enum.RoomItemCategory

fun Item.toAdminRoomItemResponse(category: RoomItemCategory, imageUrl: String): AdminRoomItemResponse =
    AdminRoomItemResponse(
        itemId = this.id,
        name = this.name,
        price = this.price,
        category = category,
        imageUrl = imageUrl,
        createdAt = this.createdAt,
    )

fun RoomItemResult.toAdminRoomItemResponse(imageUrl: String): AdminRoomItemResponse = AdminRoomItemResponse(
    itemId = this.itemId,
    name = this.name,
    price = this.price,
    category = this.category,
    imageUrl = imageUrl,
    createdAt = this.createdAt,
)
