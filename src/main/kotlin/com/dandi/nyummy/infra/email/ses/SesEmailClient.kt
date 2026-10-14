package com.dandi.nyummy.infra.email.ses

import aws.sdk.kotlin.services.sesv2.SesV2Client
import aws.sdk.kotlin.services.sesv2.model.Body
import aws.sdk.kotlin.services.sesv2.model.Content
import aws.sdk.kotlin.services.sesv2.model.Destination
import aws.sdk.kotlin.services.sesv2.model.EmailContent
import aws.sdk.kotlin.services.sesv2.model.Message
import aws.sdk.kotlin.services.sesv2.model.SendEmailRequest
import aws.smithy.kotlin.runtime.SdkBaseException
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.EmailErrorCode
import com.dandi.nyummy.infra.email.EmailClient
import com.dandi.nyummy.infra.email.EmailMessage
import com.dandi.nyummy.infra.email.EmailProperties
import com.dandi.nyummy.log.redactEmail
import com.dandi.nyummy.log.redactEmails
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import kotlin.time.TimeSource

@Component
@ConditionalOnProperty(name = ["app.email.client"], havingValue = "ses")
class SesEmailClient(private val sesV2Client: SesV2Client, private val emailProperties: EmailProperties) : EmailClient {

    companion object {
        private val logger = LoggerFactory.getLogger(SesEmailClient::class.java)
    }

    /**
     * 실패 로그에는 예외 객체 대신 가린 예외 요약을 남긴다. SES 예외 메시지에 수신자 주소가 섞여 오기 때문이다.
     *
     * @throws BusinessException [EmailErrorCode.EMAIL_SEND_FAILED] SES 발송 요청이 실패한 경우
     */
    override fun sendEmail(message: EmailMessage) {
        val startedAt = TimeSource.Monotonic.markNow()
        val request = SendEmailRequest {
            fromEmailAddress = "${emailProperties.fromName} <${emailProperties.fromAddress}>"
            destination = Destination {
                toAddresses = listOf(message.to)
            }
            content = EmailContent {
                simple = Message {
                    subject = Content { data = message.subject }
                    body = Body {
                        text = Content { data = message.text }
                    }
                }
            }
        }

        runBlocking {
            try {
                val response = sesV2Client.sendEmail(request)

                logger.debug(
                    "이메일 발송(SES): to={}, subject={}, messageId={}, elapsed={}ms",
                    redactEmail(message.to),
                    message.subject,
                    response.messageId,
                    startedAt.elapsedNow().inWholeMilliseconds,
                )
            } catch (e: SdkBaseException) {
                logger.error(
                    "이메일 발송 실패(SES): to={}, subject={}, elapsed={}ms, error={}",
                    redactEmail(message.to),
                    message.subject,
                    startedAt.elapsedNow().inWholeMilliseconds,
                    redactEmails(e.toString()),
                )
                throw BusinessException(EmailErrorCode.EMAIL_SEND_FAILED)
            }
        }
    }
}
