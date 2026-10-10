package com.dandi.nyummy.inquiry.mapper

import com.dandi.nyummy.inquiry.dto.CreateInquiryRequest
import com.dandi.nyummy.inquiry.dto.InquiryResponse
import com.dandi.nyummy.inquiry.entity.Inquiry

fun CreateInquiryRequest.toInquiry(userId: Long): Inquiry = Inquiry(
    userId = userId,
    category = this.category,
    questionTitle = this.questionTitle,
    questionContent = this.questionContent,
)

fun Inquiry.toInquiryResponse(): InquiryResponse = InquiryResponse(
    inquiryId = this.id,
    category = this.category,
    questionTitle = this.questionTitle,
    questionContent = this.questionContent,
    createdAt = this.createdAt,
    isAnswered = this.isAnswered,
    answerTitle = this.answerTitle,
    answerContent = this.answerContent,
    answeredAt = this.answeredAt,
)
