package com.dandi.nyummy.room.entity

import com.dandi.nyummy.room.enum.RoomItemCategory
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 마이룸 아이템 전용 속성. [com.dandi.nyummy.item.entity.Item]과 1:1이라 [itemId]가 PK이자 FK다.
 */
@Entity
@Table(name = "room_items")
class RoomItem(
    @Id
    @Column(name = "item_id", nullable = false)
    val itemId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    val category: RoomItemCategory,
)
