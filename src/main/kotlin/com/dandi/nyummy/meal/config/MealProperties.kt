package com.dandi.nyummy.meal.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("spring.app.meal")
data class MealProperties(val maxFileSizeBytes: Long, val presignedUrlExpirationMinutes: Int)
