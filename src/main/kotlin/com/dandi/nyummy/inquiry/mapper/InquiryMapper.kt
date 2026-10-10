package com.dandi.nyummy.inquiry.mapper

import com.dandi.nyummy.inquiry.dto.CreateInquiryRequest
import com.dandi.nyummy.inquiry.entity.Inquiry

fun CreateInquiryRequest.toInquiry(userId: Long): Inquiry = Inquiry(
    userId = userId,
    category = this.category,
    questionTitle = this.questionTitle,
    questionContent = this.questionContent,
)
