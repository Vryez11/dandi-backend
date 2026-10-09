package com.dandi.nyummy.infra.email

import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.EmailErrorCode
import org.springframework.stereotype.Service

@Service
class EmailService(private val emailClient: EmailClient) {

    /**
     * 인증 코드를 본문에 담아 이메일을 발송한다.
     *
     * @param email 수신자 이메일 주소
     * @param code 발송할 6자리 인증 코드
     * @throws BusinessException [EmailErrorCode.EMAIL_SEND_FAILED] 이메일 발송이 실패한 경우
     */
    fun sendAuthCode(email: String, code: String) {
        emailClient.send(
            EmailMessage(
                to = email,
                subject = "[Nyummy] 인증 코드",
                text = "인증 코드는 $code 입니다.",
            ),
        )
    }

    /**
     * 임시 비밀번호를 본문에 담아 이메일을 발송한다.
     *
     * @param email 수신자 이메일 주소
     * @param tempPassword 발송할 평문 임시 비밀번호
     * @throws BusinessException [EmailErrorCode.EMAIL_SEND_FAILED] 이메일 발송이 실패한 경우
     */
    fun sendTempPassword(email: String, tempPassword: String) {
        emailClient.send(
            EmailMessage(
                to = email,
                subject = "[Nyummy] 임시 비밀번호",
                text = "임시 비밀번호는 $tempPassword 입니다. 로그인 후 비밀번호를 변경해 주세요.",
            ),
        )
    }
}
