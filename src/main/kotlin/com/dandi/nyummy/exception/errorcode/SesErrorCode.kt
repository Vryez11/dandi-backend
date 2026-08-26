package com.dandi.nyummy.exception.errorcode

import org.springframework.http.HttpStatus

enum class SesErrorCode(override val status: HttpStatus, override val code: String, override val message: String) :
    ErrorCode {
    SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "api.ses.sendFailed", "메일 전송에 실패했습니다."),
}
