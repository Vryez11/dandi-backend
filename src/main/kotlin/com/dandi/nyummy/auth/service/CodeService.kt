package com.dandi.nyummy.auth.service

import com.dandi.nyummy.auth.entity.Code
import com.dandi.nyummy.auth.repository.CodeRepository
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.AuthErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant

@Service
class CodeService(
    private val codeRepository: CodeRepository,
    @Value("\${app.jwt.email-challenge-time-to-live}") private val timeToLive: Duration,
) {
    companion object {
        private val random = SecureRandom()
    }

    @Transactional
    fun createCodeByEmail(email: String): String {
        var code = codeRepository.findByEmail(email)
        val newCode = createRandomCode()

        if (code == null) {
            code = Code(
                email = email,
                code = newCode,
            )
        } else {
            if (code.createdAt.plus(timeToLive) < Instant.now()) {
                code.resetSendWindow()
            }

            code.updateCode(newCode)
        }

        code.increaseSendCount()

        if (code.sendCount > 5) {
            throw BusinessException(AuthErrorCode.MAIL_TOO_MANY_REQUEST)
        }

        return codeRepository.save(code).code
    }

    private fun createRandomCode(): String = "%06d".format(random.nextInt(1_000_000))
}
