package com.dandi.nyummy.infra.email.smtp

import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.EmailErrorCode
import com.dandi.nyummy.infra.email.EmailClient
import com.dandi.nyummy.infra.email.EmailMessage
import com.dandi.nyummy.infra.email.EmailProperties
import jakarta.mail.MessagingException
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.email.client"], havingValue = "smtp")
class SmtpEmailClient(private val javaMailSender: JavaMailSender, private val emailProperties: EmailProperties) :
    EmailClient {

    companion object {
        val log = LoggerFactory.getLogger(SmtpEmailClient::class.java)
    }

    /**
     * SimpleMailMessage는 From 표시 이름을 지원하지 않아 MimeMessageHelper로 구성한다.
     * 한글 제목·본문이 깨지지 않도록 UTF-8을 지정한다.
     *
     * @throws BusinessException [EmailErrorCode.EMAIL_SEND_FAILED] SMTP 발송 요청이 실패한 경우
     */
    override fun send(message: EmailMessage) {
        try {
            val mimeMessage = javaMailSender.createMimeMessage()
            MimeMessageHelper(mimeMessage, "UTF-8").apply {
                setFrom(emailProperties.fromAddress, emailProperties.fromName)
                setTo(message.to)
                setSubject(message.subject)
                setText(message.text)
            }
            javaMailSender.send(mimeMessage)
        } catch (e: MailException) {
            log.error("이메일 발송이 실패했습니다. ${e.message}")
            throw BusinessException(EmailErrorCode.EMAIL_SEND_FAILED)
        } catch (e: MessagingException) {
            log.error("이메일 메시지 구성이 실패했습니다. ${e.message}")
            throw BusinessException(EmailErrorCode.EMAIL_SEND_FAILED)
        }
    }
}
