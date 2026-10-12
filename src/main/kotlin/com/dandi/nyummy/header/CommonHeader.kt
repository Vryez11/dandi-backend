package com.dandi.nyummy.header

// 앱이 모든 요청에 실어 보내는 기기·환경 정보. 값은 변환·검증 없이 받은 그대로 둔다.
// 이전 앱 빌드·health check·Swagger는 보내지 않으므로 없으면 null이다.
data class CommonHeader(
    val appVersion: String?,
    val deviceModel: String?,
    val osVersion: String?,
    val language: String?,
    val region: String?,
    val timezoneId: String?,
) {
    // 로그 한 줄에 그대로 붙인다. null은 "앱이 보내지 않음"이라 그대로 남긴다.
    override fun toString(): String = "appVersion=$appVersion deviceModel=$deviceModel osVersion=$osVersion " +
        "language=$language region=$region timezoneId=$timezoneId"

    companion object {
        const val APP_VERSION = "X-App-Version"
        const val DEVICE_MODEL = "X-Device-Model"
        const val OS_VERSION = "X-OS-Version"
        const val LANGUAGE = "X-Language"
        const val REGION = "X-Region"
        const val TIMEZONE_ID = "X-Timezone-Id"
    }
}
