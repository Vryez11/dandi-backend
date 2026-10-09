package com.dandi.nyummy.infra.email

/**
 * 메일 전송 수단(SES·SMTP) 클라이언트. 무엇을 보낼지는 [EmailService]가 정하고, 구현체는 전송만 맡는다.
 *
 * 구현체는 전송 실패를 [com.dandi.nyummy.exception.errorcode.EmailErrorCode.EMAIL_SEND_FAILED]로 변환해 던진다 —
 * 호출부가 어느 구현체가 동작 중인지 몰라도 되게 하기 위함이다.
 */
interface EmailClient {

    fun send(message: EmailMessage)
}
