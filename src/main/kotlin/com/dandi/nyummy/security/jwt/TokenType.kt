package com.dandi.nyummy.security.jwt

enum class TokenType(val value: String) {
    ACCESS("access"),
    REFRESH("refresh"),
    EMAIL_CHALLENGE("emailChallenge"),
    EMAIL_VERIFIED("emailVerified"),
}
