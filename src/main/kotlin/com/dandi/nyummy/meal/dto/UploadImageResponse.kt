package com.dandi.nyummy.meal.dto

import kotlin.time.Instant

data class UploadImageResponse(
    val uploadUrl: String,
    val imageKey: String,
    val uploadMethod: String,
    val uploadHeaders: Map<String, String>,
    val expiresAt: Instant
)
