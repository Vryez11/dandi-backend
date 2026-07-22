package com.dandi.nyummy.exception

import com.dandi.nyummy.exception.errorcode.ErrorCode

data class ErrorResponse(
    val code: String,
    val message: String,
) {
    companion object {
        fun of(errorCode: ErrorCode): ErrorResponse =
            ErrorResponse(code = errorCode.code, message = errorCode.message)
        fun of(code: String, message: String): ErrorResponse =
            ErrorResponse(code = code, message = message)
    }
}
