@file:Suppress("ktlint:standard:filename")

package com.dandi.nyummy.inquiry.dto

import com.dandi.nyummy.inquiry.enum.InquiryCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateInquiryRequest(
    val category: InquiryCategory,

    @field:NotBlank(message = "문의 제목은 필수입니다.")
    @field:Size(max = 100, message = "문의 제목은 100자 이하여야 합니다.")
    val questionTitle: String,

    @field:NotBlank(message = "문의 내용은 필수입니다.")
    @field:Size(max = 2000, message = "문의 내용은 2000자 이하여야 합니다.")
    val questionContent: String,
)
