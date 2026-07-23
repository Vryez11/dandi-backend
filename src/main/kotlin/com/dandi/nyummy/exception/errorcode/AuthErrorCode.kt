package com.dandi.nyummy.exception.errorcode

import org.springframework.http.HttpStatus

enum class AuthErrorCode(override val status: HttpStatus, override val code: String, override val message: String):ErrorCode {

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "api.auth.unauthorized", "해당 요청에 대한 권한이 없습니다"),

}