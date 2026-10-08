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
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.email.client"], havingValue = "ses")
class SesEmailClient(private val sesV2Client: SesV2Client, private val emailProperties: EmailProperties) : EmailClient {

    companion object {
        val log = LoggerFactory.getLogger(SesEmailClient::class.java)
    }

    /**
     * @throws BusinessException [EmailErrorCode.EMAIL_SEND_FAILED] SES 발송 요청이 실패한 경우
     */
    override fun send(message: EmailMessage) {
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
                sesV2Client.sendEmail(request)
            } catch (e: SdkBaseException) {
                log.error("이메일 발송이 실패했습니다. ${e.message}")
                throw BusinessException(EmailErrorCode.EMAIL_SEND_FAILED)
            }
        }
    }
}
