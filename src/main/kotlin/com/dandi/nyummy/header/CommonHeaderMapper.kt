package com.dandi.nyummy.header

import jakarta.servlet.http.HttpServletRequest

fun HttpServletRequest.toCommonHeader(): CommonHeader = CommonHeader(
    appVersion = getHeaderOrNull(CommonHeader.APP_VERSION),
    deviceModel = getHeaderOrNull(CommonHeader.DEVICE_MODEL),
    osVersion = getHeaderOrNull(CommonHeader.OS_VERSION),
    language = getHeaderOrNull(CommonHeader.LANGUAGE),
    region = getHeaderOrNull(CommonHeader.REGION),
    timezoneId = getHeaderOrNull(CommonHeader.TIMEZONE_ID),
)

// 빈 문자열도 보내지 않은 것으로 본다.
private fun HttpServletRequest.getHeaderOrNull(name: String): String? = getHeader(name)?.takeIf { it.isNotBlank() }
