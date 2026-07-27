package com.dandi.nyummy.infra.ai

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "ai")
class AiProperties (
    val apiKey: String,
    val model: String,
    val baseUrl: String,
    val connectTimeout: Duration,
    val readTimeout: Duration,
)