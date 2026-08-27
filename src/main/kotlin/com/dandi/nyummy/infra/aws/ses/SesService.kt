package com.dandi.nyummy.infra.aws.ses

import aws.sdk.kotlin.services.sesv2.SesV2Client
import aws.sdk.kotlin.services.sesv2.model.Body
import aws.sdk.kotlin.services.sesv2.model.Content
import aws.sdk.kotlin.services.sesv2.model.Destination
import aws.sdk.kotlin.services.sesv2.model.EmailContent
import aws.sdk.kotlin.services.sesv2.model.Message
import aws.sdk.kotlin.services.sesv2.model.SendEmailRequest
import aws.smithy.kotlin.runtime.SdkBaseException
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.SesErrorCode
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SesService(
    private val sesV2Client: SesV2Client,
    @Value("\${AWS_SES_FROM_ADDRESS}") private val fromAddress: String,
) {

    companion object {
        val log = LoggerFactory.getLogger(SesService::class.java)
    }

    /**
     * 인증 코드를 본문에 담아 SES로 이메일을 발송한다.
     *
     * @param email 수신자 이메일 주소
     * @param code 발송할 6자리 인증 코드
     * @throws BusinessException [SesErrorCode.SEND_FAILED] SES 발송 요청이 실패한 경우
     */
    fun sendAuthCode(email: String, code: String) {
        val request = SendEmailRequest {
            fromEmailAddress = fromAddress
            destination = Destination {
                toAddresses = listOf(email)
            }
            content = EmailContent {
                simple = Message {
                    subject = Content { data = "[Nyummy] 인증 코드" }
                    body = Body {
                        text = Content { data = "인증 코드는 $code 입니다." }
                    }
                }
            }
        }

        runBlocking {
            sendEmail(request)
        }
    }

    private suspend fun sendEmail(request: SendEmailRequest) {
        try {
            sesV2Client.sendEmail(request)
        } catch (e: SdkBaseException) {
            log.error("이메일 발송이 실패했습니다. ${e.message}")
            throw BusinessException(SesErrorCode.EMAIL_SEND_FAILED)
        }
    }
}
