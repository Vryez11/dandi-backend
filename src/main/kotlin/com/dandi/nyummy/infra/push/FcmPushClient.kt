package com.dandi.nyummy.infra.push

import com.dandi.nyummy.infra.push.dto.PushMessage
import com.dandi.nyummy.infra.push.dto.PushResult
import com.dandi.nyummy.log.redactToken
import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import kotlin.time.TimeSource

@Component
@ConditionalOnProperty(prefix = "app.fcm", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class FcmPushClient(
    private val firebaseMessaging: FirebaseMessaging,
    private val fcmProperties: FcmProperties,
    private val clock: Clock,
) : PushClient {

    companion object {
        /** sendEach 한 번에 담을 수 있는 메시지 수 상한 */
        private const val SEND_EACH_LIMIT = 500

        private const val SCHEMA_VERSION = "1"
        private const val KEY_TYPE = "type"
        private const val KEY_SCHEMA_VERSION = "v"
        private const val KEY_NOTIFICATION_ID = "notificationId"
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        private const val KEY_APP_LINK = "appLink"
        private const val KEY_SENT_AT = "sentAt"

        /** 토큰 자체가 죽은 경우. 삭제 대상이며 재시도해도 소용없다. */
        private val INVALID_TOKEN_ERROR_CODES = setOf(
            MessagingErrorCode.UNREGISTERED, // 앱 삭제 또는 토큰 만료
            MessagingErrorCode.INVALID_ARGUMENT, // 토큰 형식 오류
            MessagingErrorCode.SENDER_ID_MISMATCH, // 다른 Firebase 프로젝트의 토큰
        )

        /** 실패 로그에 남길 표본 수 */
        private const val FAILURE_SAMPLE_SIZE = 3

        private val logger = LoggerFactory.getLogger(FcmPushClient::class.java)
    }

    override fun sendPushes(messagesByToken: Map<String, PushMessage>): PushResult {
        if (messagesByToken.isEmpty()) {
            return PushResult.EMPTY
        }

        return messagesByToken.toList()
            .chunked(SEND_EACH_LIMIT)
            .fold(PushResult.EMPTY) { acc, chunk -> acc + sendChunk(chunk) }
    }

    /**
     * 청크 하나를 발송한다. 메시지마다 토큰과 내용이 다르므로 [FirebaseMessaging.sendEach]를 쓴다.
     *
     * sendEach는 메시지당 요청을 하나씩 보내지만 순차가 아니다 — 전부 백그라운드 스레드에 던진 뒤
     * 한꺼번에 모은다. 직접 send()를 루프로 돌면 왕복을 차례로 기다리므로 리스트로 넘긴다.
     */
    private fun sendChunk(entries: List<Pair<String, PushMessage>>): PushResult {
        val startedAt = TimeSource.Monotonic.markNow()
        val messages = entries.map { (token, message) ->
            Message.builder()
                .setToken(token)
                .setNotification(
                    Notification.builder()
                        .setTitle(message.title)
                        .setBody(message.body)
                        .build(),
                )
                .putAllData(convertData(message))
                .build()
        }

        val response = try {
            firebaseMessaging.sendEach(messages, fcmProperties.dryRun)
        } catch (e: FirebaseMessagingException) {
            // 발송을 기다리는 것 자체가 끊긴 경우(인터럽트)
            logger.error(
                "푸시 발송 실패(FCM): messageCount={}, types={}, elapsed={}ms",
                messages.size,
                entries.map { it.second.type }.distinct(),
                startedAt.elapsedNow().inWholeMilliseconds,
                e,
            )

            return PushResult(
                successCount = 0,
                invalidTokens = emptyList(),
                retryableTokens = entries.map { it.first },
            )
        }

        return convertPushResult(entries, response, startedAt.elapsedNow().inWholeMilliseconds)
    }

    /**
     * FCM data 페이로드는 값이 전부 문자열이어야 한다. 숫자·불리언·중첩 객체는 들어가지 않는다.
     *
     * title·body는 notification 블록과 중복된다. 의도된 중복이다 — 백그라운드에서는 OS가
     * notification으로 표시하고, 포그라운드에서는 앱이 data를 읽어 직접 그린다.
     *
     * sentAt은 UTC다.
     */
    private fun convertData(message: PushMessage): Map<String, String> = mapOf(
        KEY_TYPE to message.type.name,
        KEY_SCHEMA_VERSION to SCHEMA_VERSION,
        KEY_NOTIFICATION_ID to message.notificationId,
        KEY_TITLE to message.title,
        KEY_BODY to message.body,
        KEY_APP_LINK to message.appLink,
        KEY_SENT_AT to Instant.now(clock).toString(),
    )

    /**
     * 응답은 요청한 메시지와 **같은 순서**로 돌아오므로 인덱스로 짝지어 분류한다.
     *
     * [Message]에 토큰 게터가 없어 응답만으로는 어느 토큰이 실패했는지 알 수 없다. 그래서
     * 발송에 쓴 [entries]를 그대로 받아 인덱스로 토큰을 되찾는다.
     *
     * 토큰 자체가 죽은 경우만 삭제 대상으로 고른다. 일시 장애를 삭제하면 멀쩡한 사용자의
     * 알림이 영구히 끊긴다.
     */
    private fun convertPushResult(
        entries: List<Pair<String, PushMessage>>,
        response: BatchResponse,
        elapsedMillis: Long,
    ): PushResult {
        val invalidTokens = mutableListOf<String>()
        val retryableTokens = mutableListOf<String>()
        val failedIndexes = mutableListOf<Int>()

        response.responses.forEachIndexed { index, sendResponse ->
            if (sendResponse.isSuccessful) {
                return@forEachIndexed
            }

            val token = entries[index].first
            failedIndexes.add(index)

            if (sendResponse.exception?.messagingErrorCode in INVALID_TOKEN_ERROR_CODES) {
                invalidTokens.add(token)
            } else {
                retryableTokens.add(token) // UNAVAILABLE / INTERNAL / QUOTA_EXCEEDED
            }
        }

        logger.debug(
            "푸시 발송(FCM): messageCount={}, success={}, invalid={}, retryable={}, dryRun={}, elapsed={}ms",
            entries.size,
            response.successCount,
            invalidTokens.size,
            retryableTokens.size,
            fcmProperties.dryRun,
            elapsedMillis,
        )

        logPushFailures(entries, response, failedIndexes)

        return PushResult(response.successCount, invalidTokens, retryableTokens)
    }

    /**
     * 실패를 FCM 오류 코드별로 한 줄씩 남긴다. 장애 때 실패가 수백 건이어도 로그가 그만큼 늘지 않게
     * 건수와 일부 표본(알림 종류·알림 ID·가린 토큰)만 남기고, 첫 예외를 스택 트레이스로 붙인다.
     *
     * 죽은 토큰은 사용자가 앱을 지운 것이라 예상 가능한 실패(WARN), 그 외는 FCM 장애·한도라 ERROR다.
     */
    private fun logPushFailures(
        entries: List<Pair<String, PushMessage>>,
        response: BatchResponse,
        failedIndexes: List<Int>,
    ) {
        failedIndexes
            .groupBy { response.responses[it].exception?.messagingErrorCode }
            .forEach { (errorCode, indexes) ->
                val samples = indexes.take(FAILURE_SAMPLE_SIZE).map { index ->
                    val (token, message) = entries[index]
                    "${message.type}/${message.notificationId}/${redactToken(token)}"
                }
                val firstException = response.responses[indexes.first()].exception

                if (errorCode in INVALID_TOKEN_ERROR_CODES) {
                    logger.warn(
                        "푸시 발송 실패(FCM): errorCode={}, count={}, samples={}",
                        errorCode,
                        indexes.size,
                        samples,
                        firstException,
                    )
                } else {
                    logger.error(
                        "푸시 발송 실패(FCM): errorCode={}, count={}, samples={}",
                        errorCode,
                        indexes.size,
                        samples,
                        firstException,
                    )
                }
            }
    }
}
