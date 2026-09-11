package com.dandi.nyummy.security.jwt

import com.dandi.nyummy.auth.enum.AuthPurpose
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.AuthErrorCode
import com.dandi.nyummy.security.AuthUser
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import java.util.*

@Service
class TokenService(private val jwtProvider: JwtProvider) {

    fun getAuthentication(token: String): Authentication {
        val userId = jwtProvider.getUserId(token, TokenType.ACCESS)

        return UsernamePasswordAuthenticationToken.authenticated(AuthUser(userId, token), null, emptyList())
    }

    fun createTokenPair(userId: Long): Pair<String, String> {
        val access = jwtProvider.createAccessToken(userId)
        val refresh = jwtProvider.createRefreshToken(userId)
        return Pair(access, refresh)
    }

    fun getEmailAndPurpose(token: String, type: TokenType): Pair<String, AuthPurpose> = try {
        Pair(jwtProvider.getEmail(token, type), jwtProvider.getPurpose(token, type))
    } catch (e: ExpiredJwtException) {
        throw BusinessException(AuthErrorCode.EMAIL_VERIFICATION_EXPIRED)
    } catch (e: JwtException) {
        throw BusinessException(AuthErrorCode.UNAUTHORIZED)
    }

    fun createEmailChallengeToken(email: String, purpose: AuthPurpose): String =
        jwtProvider.createEmailChallengeToken(email, purpose)

    fun createEmailVerifiedToken(email: String, purpose: AuthPurpose): String =
        jwtProvider.createEmailVerifiedToken(email, purpose)

    fun getPurpose(token: String, type: TokenType): AuthPurpose = jwtProvider.getPurpose(token, type)

    fun getUserId(token: String, type: TokenType): Long = jwtProvider.getUserId(token, type)

    fun getEmail(token: String, type: TokenType): String = jwtProvider.getEmail(token, type)

    fun getExpiration(token: String, type: TokenType): Date = jwtProvider.getExpiration(token, type)
}
