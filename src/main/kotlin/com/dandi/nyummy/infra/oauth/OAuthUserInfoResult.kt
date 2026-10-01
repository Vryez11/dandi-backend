package com.dandi.nyummy.infra.oauth

import com.dandi.nyummy.auth.enum.AuthProvider

/**
 * 제공자가 검증해 준 신원. 제공자 안에서 사용자를 유일하게 식별하는 값만 담는다.
 * 이메일·닉네임 같은 프로필은 받지 않는다 — 쓰지 않는 개인정보는 수집하지 않는다.
 */
data class OAuthUserInfoResult(val provider: AuthProvider, val providerUserId: String)
