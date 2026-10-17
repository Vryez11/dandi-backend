package com.dandi.nyummy.item.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

/**
 * 아이템(상품 정의). 도메인 고유 속성은 확장 엔티티([com.dandi.nyummy.room.entity.RoomItem])가 갖는다.
 *
 * 이미지 URL은 저장하지 않고 id로 CDN 고정 경로를 만들어 쓴다.
 * [price]가 null이면 상점에서 팔지 않는 아이템(보상 전용)이다.
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "items")
class Item(
    @Column(name = "name", nullable = false, length = 100)
    val name: String,

    @Column(name = "price")
    val price: Int? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0L

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
}
