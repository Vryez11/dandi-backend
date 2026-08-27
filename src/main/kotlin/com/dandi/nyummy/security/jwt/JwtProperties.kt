package com.dandi.nyummy.security.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.jwt")
class JwtProperties(

    val secretKey: String,

    val accessTimeToLive: Duration,

    val refreshTimeToLive: Duration,

    val emailChallengeTimeToLive: Duration,

    val emailVerifiedTimeToLive: Duration,
)
