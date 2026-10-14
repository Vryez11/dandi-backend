package com.dandi.nyummy.infra.ai.nutrition

/**
 * 영양 분석이 실패한 원인. 분석 메트릭의 cause 태그 값(소문자)으로 쓰므로 값을 함부로 늘리지 않는다.
 *
 * 특정 AI 제공자에 묶이지 않게 HTTP 응답 종류로 나눈다.
 */
enum class AnalysisFailureCause {
    NOT_FOOD, // 음식 사진이 아니라고 판정됨
    IMAGE_DOWNLOAD_ERROR, // 스토리지에서 이미지를 받지 못함
    TIMEOUT, // 연결·응답 시간 초과
    NETWORK_ERROR, // 시간 초과 외 I/O 오류
    RATE_LIMITED, // 호출 한도 초과(429)
    CLIENT_ERROR, // 429 외 4xx (키·요청 형식 오류 등)
    SERVER_ERROR, // 5xx
    EMPTY_RESPONSE, // 응답에 결과가 없음
    PARSE_ERROR, // 응답을 해석하지 못함
    UNKNOWN, // 위에 해당하지 않음
}

class NutritionAnalysisException(val failureCause: AnalysisFailureCause, message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
