package com.dandi.nyummy.infra.email.smtp

import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.EmailErrorCode
import com.dandi.nyummy.infra.email.EmailClient
import com.dandi.nyummy.infra.email.EmailMessage
import com.dandi.nyummy.infra.email.EmailProperties
import com.dandi.nyummy.log.redactEmail
import com.dandi.nyummy.log.redactEmails
import jakarta.mail.MessagingException
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import kotlin.time.TimeSource

@Component
@ConditionalOnProperty(name = ["app.email.client"], havingValue = "smtp")
class SmtpEmailClient(private val javaMailSender: JavaMailSender, private val emailProperties: EmailProperties) :
    EmailClient {

    companion object {
        private val logger = LoggerFactory.getLogger(SmtpEmailClient::class.java)
    }

    /**
     * SimpleMailMessage는 From 표시 이름을 지원하지 않아 MimeMessageHelper로 구성한다.
     * 한글 제목·본문이 깨지지 않도록 UTF-8을 지정한다.
     *
     * 실패 로그에는 예외 객체 대신 가린 예외 요약을 남긴다. SMTP 예외 메시지에 수신자 주소가 섞여 오기 때문이다.
     *
     * @throws BusinessException [EmailErrorCode.EMAIL_SEND_FAILED] SMTP 발송 요청이 실패한 경우
     */
    override fun sendEmail(message: EmailMessage) {
        val startedAt = TimeSource.Monotonic.markNow()
        try {
            val mimeMessage = javaMailSender.createMimeMessage()
            MimeMessageHelper(mimeMessage, "UTF-8").apply {
                setFrom(emailProperties.fromAddress, emailProperties.fromName)
                setTo(message.to)
                setSubject(message.subject)
                setText(message.text)
            }
            javaMailSender.send(mimeMessage)

            logger.debug(
                "이메일 발송(SMTP): to={}, subject={}, elapsed={}ms",
                redactEmail(message.to),
                message.subject,
                startedAt.elapsedNow().inWholeMilliseconds,
            )
        } catch (e: MailException) {
            logger.error(
                "이메일 발송 실패(SMTP): to={}, subject={}, elapsed={}ms, error={}",
                redactEmail(message.to),
                message.subject,
                startedAt.elapsedNow().inWholeMilliseconds,
                redactEmails(e.toString()),
            )
            throw BusinessException(EmailErrorCode.EMAIL_SEND_FAILED)
        } catch (e: MessagingException) {
            logger.error(
                "이메일 메시지 구성 실패(SMTP): to={}, subject={}, error={}",
                redactEmail(message.to),
                message.subject,
                redactEmails(e.toString()),
            )
            throw BusinessException(EmailErrorCode.EMAIL_SEND_FAILED)
        }
    }
}
