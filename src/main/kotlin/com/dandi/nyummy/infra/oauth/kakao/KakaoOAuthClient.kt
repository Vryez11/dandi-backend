package com.dandi.nyummy.infra.oauth.kakao

import com.dandi.nyummy.auth.enum.AuthProvider
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.AuthErrorCode
import com.dandi.nyummy.infra.oauth.OAuthClient
import com.dandi.nyummy.infra.oauth.OAuthUserInfoResult
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException

class KakaoOAuthClient(private val jwtDecoder: JwtDecoder) : OAuthClient {

    companion object {
        private val logger = LoggerFactory.getLogger(KakaoOAuthClient::class.java)

        private const val CLAIM_NONCE = "nonce"
    }

    override val provider: AuthProvider = AuthProvider.KAKAO

    /**
     * Kakao OIDC ID 토큰을 검증한다. 서명·발급자·대상·만료는 [jwtDecoder]가, nonce는 여기서 대조한다.
     *
     * nonce가 없으면 토큰을 이 앱 세션에 묶을 수 없으므로 검증 전에 거절한다 — 다른 경로에서 얻은 ID 토큰 주입을 막는다.
     */
    override fun getUserInfo(token: String, nonce: String?): OAuthUserInfoResult {
        val expectedNonce = nonce
            ?: throw BusinessException(AuthErrorCode.OAUTH_NONCE_REQUIRED)

        val jwt = decode(token)

        if (jwt.getClaimAsString(CLAIM_NONCE) != expectedNonce) {
            logger.warn("Kakao ID 토큰 nonce 불일치")
            throw BusinessException(AuthErrorCode.INVALID_OAUTH_TOKEN)
        }

        val providerUserId = jwt.subject
            ?: throw BusinessException(AuthErrorCode.INVALID_OAUTH_TOKEN)

        return OAuthUserInfoResult(
            provider = provider,
            providerUserId = providerUserId,
        )
    }

    private fun decode(token: String): Jwt = try {
        jwtDecoder.decode(token)
    } catch (e: BadJwtException) {
        logger.warn("Kakao ID 토큰 검증 실패: {}", e.message)
        throw BusinessException(AuthErrorCode.INVALID_OAUTH_TOKEN)
    } catch (e: JwtException) {
        logger.error("Kakao 공개키 조회 실패", e)
        throw BusinessException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE)
    }
}
