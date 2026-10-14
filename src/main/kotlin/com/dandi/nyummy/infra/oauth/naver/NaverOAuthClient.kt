package com.dandi.nyummy.infra.oauth.naver

import com.dandi.nyummy.auth.enum.AuthProvider
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.AuthErrorCode
import com.dandi.nyummy.infra.oauth.OAuthClient
import com.dandi.nyummy.infra.oauth.OAuthUserInfoResult
import org.slf4j.LoggerFactory
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import kotlin.time.TimeSource

class NaverOAuthClient(private val restClient: RestClient, private val userInfoUri: String) : OAuthClient {

    companion object {
        private val logger = LoggerFactory.getLogger(NaverOAuthClient::class.java)

        private const val RESULT_CODE_SUCCESS = "00"
    }

    override val provider: AuthProvider = AuthProvider.NAVER

    /**
     * Naver access token으로 회원 프로필 조회 API를 호출해 검증한다. Naver는 OIDC가 없어 서버가 서명을 직접 검증할 수 없다.
     *
     * 토큰에 대상(aud)이 없어 다른 앱이 발급받은 토큰도 조회에 성공하지만, 응답의 id는 애플리케이션마다 다른 값이라
     * 다른 앱의 토큰으로는 이 앱의 기존 회원과 매칭되지 않는다.
     *
     * [nonce]는 무시한다 — access token에는 nonce를 묶을 자리가 없다.
     * 403은 콘솔의 API 권한 미설정(서버 설정 문제)이므로 토큰 오류가 아닌 제공자 통신 불가로 처리한다.
     */
    override fun getUserInfo(token: String, nonce: String?): OAuthUserInfoResult {
        val response = getNaverUserInfo(token)

        if (response.resultcode != RESULT_CODE_SUCCESS) {
            logger.warn("Naver 프로필 조회 실패: resultcode={}, message={}", response.resultcode, response.message)
            throw BusinessException(AuthErrorCode.INVALID_OAUTH_TOKEN)
        }

        val providerUserId = response.response?.id
            ?: throw BusinessException(AuthErrorCode.INVALID_OAUTH_TOKEN)

        return OAuthUserInfoResult(
            provider = provider,
            providerUserId = providerUserId,
        )
    }

    private fun getNaverUserInfo(token: String): NaverUserInfoResponse {
        val startedAt = TimeSource.Monotonic.markNow()
        val response = try {
            restClient.get()
                .uri(userInfoUri)
                .headers { it.setBearerAuth(token) }
                .retrieve()
                .body(NaverUserInfoResponse::class.java)
        } catch (e: HttpClientErrorException.Unauthorized) {
            // 만료·위조 토큰. 예상 가능한 거절이라 스택 트레이스 없이 남긴다.
            logger.warn(
                "Naver access token 검증 실패: status={}, error={}, elapsed={}ms",
                e.statusCode.value(),
                e.message,
                startedAt.elapsedNow().inWholeMilliseconds,
            )
            throw BusinessException(AuthErrorCode.INVALID_OAUTH_TOKEN)
        } catch (e: HttpClientErrorException.Forbidden) {
            logger.error(
                "Naver 프로필 조회 권한 없음(개발자센터 API 권한 설정 확인 필요): elapsed={}ms",
                startedAt.elapsedNow().inWholeMilliseconds,
                e,
            )
            throw BusinessException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE)
        } catch (e: RestClientException) {
            logger.error("Naver 프로필 조회 실패: elapsed={}ms", startedAt.elapsedNow().inWholeMilliseconds, e)
            throw BusinessException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE)
        }

        if (response == null) {
            logger.error("Naver 프로필 응답이 비어 있음: elapsed={}ms", startedAt.elapsedNow().inWholeMilliseconds)
            throw BusinessException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE)
        }

        logger.debug(
            "Naver 프로필 조회: resultcode={}, elapsed={}ms",
            response.resultcode,
            startedAt.elapsedNow().inWholeMilliseconds,
        )
        return response
    }
}

data class NaverUserInfoResponse(val resultcode: String?, val message: String?, val response: NaverUserInfo?)
data class NaverUserInfo(val id: String?)
