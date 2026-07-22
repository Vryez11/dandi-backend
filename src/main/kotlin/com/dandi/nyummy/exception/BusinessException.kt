package com.dandi.nyummy.exception

import com.dandi.nyummy.exception.errorcode.ErrorCode

class BusinessException(
    val errorCode: ErrorCode,
    message: String = errorCode.message
): RuntimeException(message) {}