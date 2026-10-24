package com.dandi.nyummy.exception.errorcode

import org.springframework.http.HttpStatus

enum class CdnErrorCode(override val status: HttpStatus, override val code: String, override val message: String) :
    ErrorCode {
    INVALIDATION_FAILED(
        HttpStatus.BAD_GATEWAY,
        "api.cdn.invalidationFailed",
        "파일은 바뀌었지만 CDN에 반영하지 못했습니다. 같은 파일로 다시 시도해 주세요.",
    ),
}
