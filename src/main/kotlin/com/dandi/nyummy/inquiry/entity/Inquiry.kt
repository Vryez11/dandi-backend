package com.dandi.nyummy.inquiry.entity

import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.InquiryErrorCode
import com.dandi.nyummy.inquiry.enum.InquiryCategory
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

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "inquiries")
class Inquiry(

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    var category: InquiryCategory,

    @Column(name = "question_title", nullable = false, length = 100)
    var questionTitle: String,

    @Column(name = "question_content", nullable = false, length = 2000)
    var questionContent: String,

    @Column(name = "image_key", length = 512)
    var imageKey: String? = null,

    @Column(name = "app_version", length = 20)
    val appVersion: String? = null,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0L

    @Column(name = "answer_title", length = 100)
    var answerTitle: String? = null

    @Column(name = "answer_content", length = 2000)
    var answerContent: String? = null

    @Column(name = "answered_at")
    var answeredAt: Instant? = null

    @Column(name = "answer_read_at")
    var answerReadAt: Instant? = null

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null

    val isAnswered: Boolean
        get() = answeredAt != null

    fun updateQuestion(category: InquiryCategory, questionTitle: String, questionContent: String) {
        validateUpdatable()
        this.category = category
        this.questionTitle = questionTitle
        this.questionContent = questionContent
    }

    /**
     * 답변을 처음 읽은 시각을 기록한다. 답변이 없거나 이미 읽었으면 그대로 둔다.
     */
    fun updateAnswerReadAt(answerReadAt: Instant) {
        if (isAnswered && this.answerReadAt == null) {
            this.answerReadAt = answerReadAt
        }
    }

    fun updateDeletedAt(deletedAt: Instant) {
        this.deletedAt = deletedAt
    }

    fun validateOwnership(requestUserId: Long) {
        if (userId != requestUserId) {
            throw BusinessException(InquiryErrorCode.INQUIRY_NOT_FOUND)
        }
    }

    fun validateUpdatable() {
        if (isAnswered) {
            throw BusinessException(InquiryErrorCode.ALREADY_ANSWERED)
        }
    }
}
