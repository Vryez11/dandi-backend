package com.dandi.nyummy.log

// 로그는 국외 관측 서비스(Grafana Cloud)로 나가므로 개인정보는 일부만 남긴다. 데이터 자체는 바꾸지 않는다.

private val EMAIL_PATTERN = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")

private const val EMAIL_VISIBLE_LENGTH = 2
private const val TOKEN_VISIBLE_LENGTH = 6
private const val REDACTED = "***"

/** `vryez11@gmail.com` → `vr***@gmail.com`. 도메인은 장애 범위(특정 메일 서비스) 파악에 필요해 남긴다. */
fun redactEmail(email: String): String {
    val at = email.indexOf('@')
    if (at < 0) {
        return REDACTED
    }

    return email.take(at).take(EMAIL_VISIBLE_LENGTH) + REDACTED + email.substring(at)
}

/** 문장 안의 이메일 주소를 모두 가린다. 외부 SDK 예외 메시지처럼 주소가 섞여 오는 문자열에 쓴다. */
fun redactEmails(text: String?): String? = text?.replace(EMAIL_PATTERN) { redactEmail(it.value) }

/** 기기 토큰은 끝 몇 자리만 남긴다. 같은 토큰이 반복 실패하는지 대조할 수 있을 만큼만 노출한다. */
fun redactToken(token: String): String = REDACTED + token.takeLast(TOKEN_VISIBLE_LENGTH)
