package com.dandi.nyummy.auth.service

import com.dandi.nyummy.auth.entity.Code
import com.dandi.nyummy.auth.enum.AuthPurpose
import com.dandi.nyummy.auth.repository.CodeRepository
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.AuthErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Service
class CodeService(
    private val codeRepository: CodeRepository,
    private val clock: Clock,
    @Value("\${app.jwt.email-challenge-time-to-live}") private val challengeTimeToLive: Duration,
) {
    companion object {
        private val random = SecureRandom()
    }

    /**
     * 이메일에 대한 6자리 인증 코드를 발급 용도와 함께 생성해 저장한다.
     * 기존 코드가 있으면 새 코드로 교체하고, 발송 윈도우(TTL)가 지났으면 발송 횟수를 초기화한다.
     * 용도는 항상 마지막 발송 기준으로 갱신된다 — 이메일당 코드가 한 건이므로 최신 발송이 인증 세션을 정의한다.
     *
     * @param email 인증 코드를 발급할 이메일
     * @param type 인증 코드의 발급 용도
     * @return 생성된 6자리 인증 코드
     * @throws BusinessException [AuthErrorCode.EMAIL_SEND_RATE_LIMITED] TTL 윈도우 내 발송 횟수가 5회를 초과한 경우
     */
    @Transactional
    fun createCodeByEmail(email: String, type: AuthPurpose): String {
        val newCode = createRandomCode()
        val expiresAt = Instant.now(clock).plus(challengeTimeToLive)

        val code = codeRepository.findByEmail(email)
            ?: Code(
                email = email,
                code = newCode,
                expiresAt = expiresAt,
                type = type,
            )

        if (code.expiresAt < Instant.now(clock)) {
            code.resetSendCount()
            code.updateExpiresAt(expiresAt)
        }

        if (code.sendCount >= 5) {
            throw BusinessException(AuthErrorCode.EMAIL_SEND_RATE_LIMITED)
        }

        code.updateCode(newCode)
        code.updateType(type)
        code.resetAttemptCount()
        code.increaseSendCount()

        codeRepository.save(code)
        return code.code
    }

    /**
     * 이메일로 발급된 인증 코드와 입력 코드를 대조하고, 성공 시 코드를 삭제해 재사용을 막는다.
     *
     * 저장된 코드의 발급 용도가 요청 용도와 다르면 해당 용도의 코드가 없는 것으로 본다.
     * 다른 용도의 발송이 코드를 덮어쓴 뒤 이전 세션으로 confirm하는 경우를 차단한다 (attemptCount 미증가).
     * 오답은 attemptCount로 기록하며, 5회 누적되면 이후 시도는 정답이어도 차단한다.
     * 오답 예외가 나가도 attemptCount 증가가 커밋되도록 [BusinessException]은 롤백하지 않는다.
     *
     * @param challengeCode 사용자가 입력한 6자리 인증 코드
     * @param email 인증 코드를 대조할 이메일
     * @param type 인증 코드를 대조할 발급 용도
     * @throws BusinessException [AuthErrorCode.INCORRECT_EMAIL] 해당 이메일·용도로 발급된 인증 코드가 없는 경우
     * @throws BusinessException [AuthErrorCode.EMAIL_CODE_ATTEMPT_EXCEEDED] 오답이 5회 누적된 경우
     * @throws BusinessException [AuthErrorCode.EMAIL_CODE_MISMATCH] 인증 코드가 일치하지 않는 경우
     */
    @Transactional(noRollbackFor = [BusinessException::class])
    fun confirmAuthCodeByEmail(challengeCode: String, email: String, type: AuthPurpose) {
        val existingCode = codeRepository.findByEmail(email)
            ?: throw BusinessException(AuthErrorCode.INCORRECT_EMAIL)

        if (existingCode.type != type) {
            throw BusinessException(AuthErrorCode.INCORRECT_EMAIL)
        }

        val attemptCount = existingCode.attemptCount
        if (attemptCount >= 5) {
            throw BusinessException(AuthErrorCode.EMAIL_CODE_ATTEMPT_EXCEEDED)
        }

        if (existingCode.code != challengeCode) {
            existingCode.increaseAttemptCount()
            throw BusinessException(AuthErrorCode.EMAIL_CODE_MISMATCH)
        }

        codeRepository.delete(existingCode)
    }

    private fun createRandomCode(): String = "%06d".format(random.nextInt(1_000_000))
}
