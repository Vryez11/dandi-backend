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
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SesEmailClient(
    private val sesV2Client: SesV2Client,
    @Value("\${AWS_SES_FROM_ADDRESS}") private val fromAddress: String,
) : EmailClient {

    companion object {
        val log = LoggerFactory.getLogger(SesEmailClient::class.java)
    }

    /**
     * @throws BusinessException [EmailErrorCode.EMAIL_SEND_FAILED] SES 발송 요청이 실패한 경우
     */
    override fun send(message: EmailMessage) {
        val request = SendEmailRequest {
            fromEmailAddress = fromAddress
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
