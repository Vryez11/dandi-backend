package com.dandi.nyummy.security.jwt

import com.dandi.nyummy.auth.enum.AuthProvider
import com.dandi.nyummy.auth.enum.AuthPurpose
import com.dandi.nyummy.auth.repository.TokenInvalidationRepository
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.AuthErrorCode
import com.dandi.nyummy.security.AuthUser
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TokenService(
    private val jwtProvider: JwtProvider,
    private val tokenInvalidationRepository: TokenInvalidationRepository,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TokenService::class.java)
    }

    fun getAuthentication(token: String): Authentication {
        val (userId, issuedAt) = jwtProvider.getAccessTokenClaims(token)

        val invalidatedAt = try {
            tokenInvalidationRepository.getInvalidatedAt(userId)
        } catch (e: DataAccessException) {
            logger.warn("토큰 무효화 조회 실패: userId={}", userId, e)
            null
        }

        if (invalidatedAt != null && issuedAt.isBefore(invalidatedAt)) {
            throw BusinessException(AuthErrorCode.UNAUTHORIZED)
        }

        return UsernamePasswordAuthenticationToken.authenticated(AuthUser(userId, token), null, emptyList())
    }

    fun createTokenPair(userId: Long): Pair<String, String> {
        val access = jwtProvider.createAccessToken(userId)
        val refresh = jwtProvider.createRefreshToken(userId)
        return Pair(access, refresh)
    }

    fun createEmailChallengeToken(email: String, purpose: AuthPurpose): String =
        jwtProvider.createEmailChallengeToken(email, purpose)

    fun getEmailChallengeClaims(token: String): EmailChallengeClaims = try {
        jwtProvider.getEmailChallengeClaims(token)
    } catch (e: ExpiredJwtException) {
        throw BusinessException(AuthErrorCode.EMAIL_CODE_EXPIRED)
    } catch (e: JwtException) {
        throw BusinessException(AuthErrorCode.INVALID_EMAIL_CHALLENGE_TOKEN)
    }

    fun createVerifiedToken(claims: VerifiedClaims): String = jwtProvider.createVerifiedToken(claims)

    fun getVerifiedClaims(token: String): VerifiedClaims = try {
        jwtProvider.getVerifiedClaims(token)
    } catch (e: ExpiredJwtException) {
        throw BusinessException(AuthErrorCode.VERIFICATION_EXPIRED)
    } catch (e: JwtException) {
        throw BusinessException(AuthErrorCode.INVALID_VERIFIED_TOKEN)
    }

    fun getUserId(token: String, type: TokenType): Long = jwtProvider.getUserId(token, type)
}

data class AccessTokenClaims(val userId: Long, val issuedAt: Instant)

data class EmailChallengeClaims(val email: String, val purpose: AuthPurpose)

data class VerifiedClaims(
    val provider: AuthProvider,
    val providerUserId: String? = null,
    val email: String? = null,
    val purpose: AuthPurpose,
)
