package com.dandi.nyummy.room.mapper

import com.dandi.nyummy.room.dto.EquippedRoomItemResponse
import com.dandi.nyummy.room.dto.EquippedRoomItemResult
import com.dandi.nyummy.room.dto.OwnedRoomItemResponse
import com.dandi.nyummy.room.dto.OwnedRoomItemResult

fun OwnedRoomItemResult.toOwnedRoomItemResponse(): OwnedRoomItemResponse = OwnedRoomItemResponse(
    ownedRoomItemId = this.ownedRoomItemId,
    name = this.name,
    itemId = this.itemId,
    isEquipped = this.isEquipped,
)

fun EquippedRoomItemResult.toEquippedRoomItemResponse(): EquippedRoomItemResponse = EquippedRoomItemResponse(
    ownedRoomItemId = this.ownedRoomItemId,
    category = this.category,
    itemId = this.itemId,
    x = this.x,
    y = this.y,
    zIndex = this.zIndex,
    scale = this.scale,
)
