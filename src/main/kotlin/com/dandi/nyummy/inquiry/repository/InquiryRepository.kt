package com.dandi.nyummy.inquiry.repository

import com.dandi.nyummy.inquiry.entity.Inquiry
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface InquiryRepository : JpaRepository<Inquiry, Long> {
    fun getInquiryByIdAndDeletedAtIsNull(inquiryId: Long): Inquiry?

    fun existsByImageKey(imageKey: String): Boolean

    /**
     * id가 [cursor]보다 작은 문의를 최신순(id 내림차순)으로 조회한다.
     *
     * 위치는 cursor 조건이 정하므로 [pageable]은 page 0으로 개수만 정한다.
     * Slice는 size + 1개를 읽어 다음 묶음이 있는지 판단한다.
     */
    @Query(
        """
        select i
        from Inquiry as i
        where i.userId = :userId
            and i.deletedAt is null
            and i.id < :cursor
        order by i.id desc
    """,
    )
    fun getInquiriesByUserIdBeforeCursor(
        @Param("userId") userId: Long,
        @Param("cursor") cursor: Long,
        pageable: Pageable,
    ): Slice<Inquiry>

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
