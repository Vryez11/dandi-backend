package com.dandi.nyummy.inquiry.repository

import com.dandi.nyummy.inquiry.entity.Inquiry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface InquiryRepository : JpaRepository<Inquiry, Long> {
    fun getInquiryByIdAndDeletedAtIsNull(inquiryId: Long): Inquiry?

    @Query(
        """
        select COUNT(i) > 0
        from Inquiry as i
        where i.userId = :userId
            and i.deletedAt is null
            and i.answeredAt is not null
            and i.answerReadAt is null
    """,
    )
    fun existsUnreadAnswerByUserId(@Param("userId") userId: Long): Boolean
}
