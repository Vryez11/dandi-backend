package com.dandi.nyummy.exception.errorcode

import org.springframework.http.HttpStatus

enum class AuthErrorCode(override val status: HttpStatus, override val code: String, override val message: String) :
    ErrorCode {
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "api.auth.invalidCredentials", "이메일 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "api.auth.unauthorized", "인증이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "api.auth.tokenExpired", "토큰이 만료되었습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "api.auth.forbidden", "해당 요청에 대한 권한이 없습니다."),
    EMAIL_SEND_RATE_LIMITED(
        HttpStatus.TOO_MANY_REQUESTS,
        "api.auth.emailSendRateLimited",
        "인증 코드 발송 시간이 일정 시간 지나지 않았습니다.",
    ),
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "api.auth.emailNotFound", "해당 이메일로 발송된 인증 코드가 없습니다."),
    EMAIL_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "api.auth.emailCodeExpired", "인증 코드 유효 시간이 지났습니다. 코드를 재발송 받으세요."),
    EMAIL_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "api.auth.emailCodeMismatch", "인증 코드가 일치하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "api.auth.invalidRefreshToken", "유효하지 않은 리프레시 토큰입니다."),
}
