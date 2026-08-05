package com.dandi.nyummy.security.jwt

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtProvider(private val jwtProperties: JwtProperties, private val clock: Clock) {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secretKey))
    private val parser: JwtParser = Jwts.parser()
        .verifyWith(secretKey)
        .clockSkewSeconds(60)
        .clock { Date.from(clock.instant()) }
        .build()

    fun createAccessToken(userId: Long): String = createToken(userId, "access", jwtProperties.accessTimeToLive)

    fun createRefreshToken(userId: Long): String = createToken(userId, "refresh", jwtProperties.refreshTimeToLive)

    private fun createToken(userId: Long, type: String, timeToLive: Duration): String {
        val now = clock.instant()

        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", type)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(timeToLive)))
            .signWith(secretKey)
            .compact()
    }

    fun getUserId(token: String): Long {
        val claims = parser.parseSignedClaims(token).payload

        if (claims["type"] != "access") {
            throw JwtException("access 토큰이 아님")
        }

        return claims.subject?.toLong() ?: throw JwtException("유효한 sub이 아님")
    }
}
