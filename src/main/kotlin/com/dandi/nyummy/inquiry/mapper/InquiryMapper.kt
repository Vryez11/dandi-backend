package com.dandi.nyummy.inquiry.mapper

import com.dandi.nyummy.inquiry.dto.CreateInquiryRequest
import com.dandi.nyummy.inquiry.dto.InquiryResponse
import com.dandi.nyummy.inquiry.dto.InquirySummaryResponse
import com.dandi.nyummy.inquiry.entity.Inquiry

fun CreateInquiryRequest.toInquiry(userId: Long, imageKey: String?, appVersion: String?): Inquiry = Inquiry(
    userId = userId,
    category = this.category,
    questionTitle = this.questionTitle,
    questionContent = this.questionContent,
    imageKey = imageKey,
    appVersion = appVersion,
)

fun Inquiry.toInquirySummaryResponse(): InquirySummaryResponse = InquirySummaryResponse(
    inquiryId = this.id,
    category = this.category,
    questionTitle = this.questionTitle,
    createdAt = this.createdAt,
    isAnswered = this.isAnswered,
    hasUnreadAnswer = this.hasUnreadAnswer,
)

fun Inquiry.toInquiryResponse(imageUrl: String?): InquiryResponse = InquiryResponse(
    inquiryId = this.id,
    category = this.category,
    questionTitle = this.questionTitle,
    questionContent = this.questionContent,
    imageUrl = imageUrl,
    createdAt = this.createdAt,
    isAnswered = this.isAnswered,
    answerTitle = this.answerTitle,
    answerContent = this.answerContent,
    answeredAt = this.answeredAt,
)
