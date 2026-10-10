package com.dandi.nyummy.exception.errorcode

import org.springframework.http.HttpStatus

enum class InquiryErrorCode(override val status: HttpStatus, override val code: String, override val message: String) :
    ErrorCode {
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "api.inquiry.notFound", "요청한 inquiryId가 데이터베이스에 존재하지 않습니다."),
}
