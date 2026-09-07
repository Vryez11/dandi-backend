package com.dandi.nyummy.security.jwt

import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.AuthErrorCode
import io.jsonwebtoken.Claims
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

        if (claims["type"] != type.value) {
            throw BusinessException(AuthErrorCode.UNAUTHORIZED)
        }

        return claims
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

    private fun createToken(email: String, type: TokenType): String {
        val now = clock.instant()

        val timeToLive = getTimeToLive(type)

        return Jwts.builder()
            .subject(email)
            .claim("type", type.value)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(timeToLive)))
            .encryptWith(encryptionKey, Jwts.ENC.A256GCM)
            .compact()
    }

    private fun getTimeToLive(type: TokenType): Duration = when (type) {
        TokenType.ACCESS -> jwtProperties.accessTimeToLive
        TokenType.REFRESH -> jwtProperties.refreshTimeToLive
        TokenType.EMAIL_CHALLENGE -> jwtProperties.emailChallengeTimeToLive
        TokenType.EMAIL_VERIFIED -> jwtProperties.emailVerifiedTimeToLive
    }

    fun createEmailChallengeToken(email: String): String = createToken(email, TokenType.EMAIL_CHALLENGE)

    fun createEmailVerifiedToken(email: String): String = createToken(email, TokenType.EMAIL_VERIFIED)

    fun createAccessToken(userId: Long): String = createToken(userId, TokenType.ACCESS)

    fun createRefreshToken(userId: Long): String = createToken(userId, TokenType.REFRESH)

    fun getUserId(token: String, type: TokenType): Long = getClaims(token, type).subject?.toLongOrNull()
        ?: throw BusinessException(AuthErrorCode.UNAUTHORIZED)

    fun getEmail(token: String, type: TokenType): String = getClaims(token, type).subject
        ?: throw BusinessException(AuthErrorCode.UNAUTHORIZED)

    fun getExpiration(token: String, type: TokenType): Date = getClaims(token, type).expiration
}
