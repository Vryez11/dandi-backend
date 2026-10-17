package com.dandi.nyummy.coin.entity

import com.dandi.nyummy.coin.enum.CoinHistoryCategory
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
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

/**
 * 코인 변동 내역. 추가만 하고 수정·삭제하지 않는다.
 *
 * [quantity]는 부호가 있다(+ 지급, - 차감). [balanceAfter]는 변동 직후 잔액이다.
 * [sourceId]는 변동을 일으킨 행의 id이고, 어느 테이블의 행인지는 [category]가 정한다.
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "coin_histories")
class CoinHistory(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    val category: CoinHistoryCategory,

    @Column(name = "quantity", nullable = false)
    val quantity: Int,

    @Column(name = "balance_after", nullable = false)
    val balanceAfter: Int,

    @Column(name = "source_id")
    val sourceId: Long? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0L

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
}
