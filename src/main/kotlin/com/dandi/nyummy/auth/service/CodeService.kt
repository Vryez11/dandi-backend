package com.dandi.nyummy.auth.service

import com.dandi.nyummy.auth.entity.Code
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
     * 이메일에 대한 6자리 인증 코드를 생성해 저장한다.
     * 기존 코드가 있으면 새 코드로 교체하고, 발송 윈도우(TTL)가 지났으면 발송 횟수를 초기화한다.
     *
     * @param email 인증 코드를 발급할 이메일
     * @return 생성된 6자리 인증 코드
     * @throws BusinessException [AuthErrorCode.MAIL_TOO_MANY_REQUEST] TTL 윈도우 내 발송 횟수가 5회를 초과한 경우
     */
    @Transactional
    fun createCodeByEmail(email: String): String {
        val newCode = createRandomCode()
        val expiresAt = Instant.now(clock).plus(challengeTimeToLive)

        val code = codeRepository.findByEmail(email)
            ?: Code(
                email = email,
                code = newCode,
                expiresAt = expiresAt,
            )

        if (code.expiresAt < Instant.now(clock)) {
            code.resetSendCount()
            code.updateExpiresAt(expiresAt)
        }

        if (code.sendCount >= 5) {
            throw BusinessException(AuthErrorCode.EMAIL_SEND_RATE_LIMITED)
        }

        code.updateCode(newCode)
        code.increaseSendCount()

        codeRepository.save(code)
        return code.code
    }

    private fun createRandomCode(): String = "%06d".format(random.nextInt(1_000_000))
}
