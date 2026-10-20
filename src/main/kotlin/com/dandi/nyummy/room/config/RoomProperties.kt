package com.dandi.nyummy.room.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.room")
data class RoomProperties(val defaultRoomItems: List<DefaultRoomItemProperties>) {
    // 같은 아이템을 두 번 적으면 가입할 때마다 한 개가 더 생기므로 앱이 뜰 때 막는다.
    init {
        require(defaultRoomItems.map { it.itemId }.toSet().size == defaultRoomItems.size) {
            "app.room.default-room-items에 같은 itemId가 두 번 있습니다: ${defaultRoomItems.map { it.itemId }}"
        }
    }
}
