package com.dandi.nyummy.infra.oauth.naver

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.oauth.naver")
class NaverOAuthProperties(val userInfoUri: String, val connectTimeout: Duration, val readTimeout: Duration)
