package com.dandi.nyummy.room.dto

/** 보유 아이템 목록 조회 결과 한 건. 상품 이름은 items에서 함께 읽는다. */
data class OwnedRoomItemResult(val roomItemId: Long, val name: String, val itemId: Long, val isEquipped: Boolean)
