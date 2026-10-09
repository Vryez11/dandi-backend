package com.dandi.nyummy.security.jwt

import com.dandi.nyummy.auth.enum.AuthProvider
import com.dandi.nyummy.auth.enum.AuthPurpose
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.AuthErrorCode
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.util.*
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.enums.enumEntries

@Component
class JwtProvider(private val jwtProperties: JwtProperties, private val clock: Clock) {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secretKey))
    private val encryptionKey: SecretKey = SecretKeySpec(Decoders.BASE64.decode(jwtProperties.encryptionKey), "AES")

    private val jwsParser: JwtParser = Jwts.parser()
        .verifyWith(secretKey)
        .clockSkewSeconds(60)
        .clock { Date.from(clock.instant()) }
        .build()

    private val jweParser: JwtParser = Jwts.parser()
        .decryptWith(encryptionKey)
        .clockSkewSeconds(60)
        .clock { Date.from(clock.instant()) }
        .build()

    private fun getClaims(token: String, type: TokenType): Claims {
        val claims = if (type.isEncrypted) {
            jweParser.parseEncryptedClaims(token).payload
        } else {
            jwsParser.parseSignedClaims(token).payload
        }

        // 여러 토큰 타입이 공유하는 경로라 에러 코드를 정하지 않고 JwtException으로 올린다 — 호출부가 토큰별 코드로 변환
        if (claims["type"] != type.value) {
            throw JwtException("토큰 타입이 일치하지 않습니다: expected=${type.value}")
        }

        return claims
    }

    /**
     * AccessToken에서 사용자 ID와 발급 시각(iat)을 함께 꺼낸다.
     *
     * 둘을 따로 조회하면 매 요청 토큰을 두 번 파싱해 서명 검증도 두 번 하게 되므로, 한 번에 읽는다.
     */
    fun getAccessTokenClaims(token: String): AccessTokenClaims {
        val claims = getClaims(token, TokenType.ACCESS)
        val userId = claims.subject?.toLongOrNull() ?: throw BusinessException(AuthErrorCode.UNAUTHORIZED)
        return AccessTokenClaims(userId, claims.issuedAt.toInstant())
    }

    private fun createToken(userId: Long, type: TokenType): String {
        val now = clock.instant()

        val timeToLive = getTimeToLive(type)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", type.value)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(timeToLive)))
            .signWith(secretKey)
            .compact()
    }

    private fun createEncryptedToken(subject: String?, type: TokenType, claims: Map<String, String>): String {
        val now = clock.instant()

        val timeToLive = getTimeToLive(type)

        return Jwts.builder()
            .subject(subject)
            .claim("type", type.value)
            .claims(claims)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(timeToLive)))
            .encryptWith(encryptionKey, Jwts.ENC.A256GCM)
            .compact()
    }

    private fun getTimeToLive(type: TokenType): Duration = when (type) {
        TokenType.ACCESS -> jwtProperties.accessTimeToLive
        TokenType.REFRESH -> jwtProperties.refreshTimeToLive
        TokenType.EMAIL_CHALLENGE -> jwtProperties.emailChallengeTimeToLive
        TokenType.VERIFIED -> jwtProperties.verifiedTimeToLive
    }

    fun createEmailChallengeToken(email: String, purpose: AuthPurpose): String =
        createEncryptedToken(email, TokenType.EMAIL_CHALLENGE, mapOf("purpose" to purpose.name))

    fun getEmailChallengeClaims(token: String): EmailChallengeClaims {
        val claims = getClaims(token, TokenType.EMAIL_CHALLENGE)

        val email = claims.subject
            ?: throw BusinessException(AuthErrorCode.INVALID_EMAIL_CHALLENGE_TOKEN)

        val purpose = claims.getEnum<AuthPurpose>("purpose")
            ?: throw BusinessException(AuthErrorCode.INVALID_EMAIL_CHALLENGE_TOKEN)

        return EmailChallengeClaims(email, purpose)
    }

    fun createVerifiedToken(claims: VerifiedClaims): String = createEncryptedToken(
        subject = null,
        type = TokenType.VERIFIED,
        claims = buildMap {
            put("provider", claims.provider.name)
            put("purpose", claims.purpose.name)
            claims.providerUserId?.let { put("providerUserId", it) }
            claims.email?.let { put("email", it) }
        },
    )

    fun getVerifiedClaims(token: String): VerifiedClaims {
        val claims = getClaims(token, TokenType.VERIFIED)

        val provider = claims.getEnum<AuthProvider>("provider")
            ?: throw BusinessException(AuthErrorCode.INVALID_VERIFIED_TOKEN)

        val purpose = claims.getEnum<AuthPurpose>("purpose")
            ?: throw BusinessException(AuthErrorCode.INVALID_VERIFIED_TOKEN)

        return VerifiedClaims(
            provider = provider,
            providerUserId = claims["providerUserId"] as? String,
            email = claims["email"] as? String,
            purpose = purpose,
        )
    }

    fun createAccessToken(userId: Long): String = createToken(userId, TokenType.ACCESS)

    fun createRefreshToken(userId: Long): String = createToken(userId, TokenType.REFRESH)

    fun getUserId(token: String, type: TokenType): Long = getClaims(token, type).subject?.toLongOrNull()
        ?: throw BusinessException(AuthErrorCode.UNAUTHORIZED)

    private inline fun <reified E : Enum<E>> Claims.getEnum(name: String): E? {
        val value = this[name] as? String ?: return null

        return enumEntries<E>().find { it.name == value }
    }
}
