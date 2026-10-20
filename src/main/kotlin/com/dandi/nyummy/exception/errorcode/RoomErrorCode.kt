package com.dandi.nyummy.exception.errorcode

import org.springframework.http.HttpStatus

enum class RoomErrorCode(override val status: HttpStatus, override val code: String, override val message: String) :
    ErrorCode {
    OWNED_ROOM_ITEM_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "api.room.ownedRoomItemNotFound",
        "요청한 ownedRoomItemId가 데이터베이스에 존재하지 않습니다.",
    ),
    DUPLICATE_OWNED_ROOM_ITEM(
        HttpStatus.BAD_REQUEST,
        "api.room.duplicateOwnedRoomItem",
        "같은 ownedRoomItemId가 두 번 이상 있습니다.",
    ),
}
