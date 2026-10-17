package com.dandi.nyummy.room.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

/**
 * 보유 아이템. 같은 아이템을 여러 개 사면 한 개마다 따로 존재하며 좌표·장착 여부를 따로 가진다.
 *
 * [id]가 API의 roomItemId다. 상품은 [itemId]로 가리킨다.
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "owned_room_items")
class OwnedRoomItem(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "item_id", nullable = false)
    val itemId: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0L

    @Column(name = "is_equipped", nullable = false)
    var isEquipped: Boolean = false

    @Column(name = "x", nullable = false)
    var x: Float = 0f

    @Column(name = "y", nullable = false)
    var y: Float = 0f

    @Column(name = "z_index", nullable = false)
    var zIndex: Int = 0

    @Column(name = "scale", nullable = false)
    var scale: Float = 1f

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
}
