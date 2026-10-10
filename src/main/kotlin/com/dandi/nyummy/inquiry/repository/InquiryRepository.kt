package com.dandi.nyummy.inquiry.repository

import com.dandi.nyummy.inquiry.entity.Inquiry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InquiryRepository : JpaRepository<Inquiry, Long> {
    fun getInquiryByIdAndDeletedAtIsNull(inquiryId: Long): Inquiry?
}
