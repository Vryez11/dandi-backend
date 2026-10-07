package com.dandi.nyummy.infra.oauth.google

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.oauth.google")
class GoogleOAuthProperties(
    val issuers: List<String>,
    val jwkSetUri: String,
    val clientIds: List<String>,
    val connectTimeout: Duration,
    val readTimeout: Duration,
)
