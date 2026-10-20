package com.dandi.nyummy.room.config

/**
 * 가입할 때 장착한 채로 지급하는 기본 아이템 하나. [x]·[y]·[zIndex]·[scale]은 꾸민 방 저장 API와 같은 기준이다.
 */
data class DefaultRoomItemProperties(val itemId: Long, val x: Float, val y: Float, val zIndex: Int, val scale: Float) {
    init {
        require(scale > 0) { "scale은 0보다 커야 합니다: itemId=$itemId scale=$scale" }
    }
}
