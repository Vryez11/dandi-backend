package com.dandi.nyummy.infra.oauth

import com.dandi.nyummy.auth.enum.AuthProvider

/**
 * 소셜 제공자별 토큰 검증 클라이언트.
 *
 * [token]의 의미는 제공자마다 다르다 — OIDC 제공자(Kakao·Google·Apple)는 ID 토큰, Naver처럼 OIDC가 없는 제공자는 access token.
 * [nonce]는 OIDC 제공자가 토큰을 앱 세션에 묶는 데 쓰며, 필요 없는 제공자는 무시한다.
 */
interface OAuthClient {

    val provider: AuthProvider

    fun getUserInfo(token: String, nonce: String?): OAuthUserInfoResult
}
