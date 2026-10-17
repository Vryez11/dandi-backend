package com.dandi.nyummy.mail.entity

import com.dandi.nyummy.mail.enum.MailCategory
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

/**
 * 우편. 보상 내용은 [category]에 따라 다르다.
 * COIN은 [coinQuantity]만, ITEM은 [MailItem]만, REWARD_BOX는 둘 다 가질 수 있다.
 *
 * ITEM 우편의 목록 이미지는 우편을 만들 때 정한 [representativeItemId]로 만든다.
 * [expiresAt]이 null이면 만료되지 않는 우편이다.
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "mails")
class Mail(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    val category: MailCategory,

    @Column(name = "title", nullable = false, length = 100)
    val title: String,

    @Column(name = "content", nullable = false, length = 255)
    val content: String,

    @Column(name = "coin_quantity")
    val coinQuantity: Int? = null,

    @Column(name = "representative_item_id")
    val representativeItemId: Long? = null,

    @Column(name = "expires_at")
    val expiresAt: Instant? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0L

    @Column(name = "claimed_at")
    var claimedAt: Instant? = null

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null

    val isClaimed: Boolean
        get() = claimedAt != null
}
