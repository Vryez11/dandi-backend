package com.dandi.nyummy.auth.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "code")
@EntityListeners(AuditingEntityListener::class)
class Code(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0L,

    @Column(name = "email", nullable = false, unique = true)
    val email: String,

    @Column(name = "code", nullable = false)
    var code: String,

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0,

    @Column(name = "send_count", nullable = false)
    var sendCount: Int = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
) {

    fun updateCode(code: String) {
        this.code = code
    }

    fun resetSendWindow() {
        this.sendCount = 0
        this.createdAt = Instant.now()
    }

    fun increaseSendCount() {
        this.sendCount += 1
    }
}
