package com.dandi.nyummy.exception.errorcode

import org.springframework.http.HttpStatus

enum class CommonErrorCode(override val status: HttpStatus, override val code: String, override val message: String) :
    ErrorCode {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "api.common.invalidInputValue", "요청 값이 올바르지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "api.common.notFound", "요청한 경로를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "api.common.internalServerError", "서버 내부 오류가 발생했습니다."),
}
