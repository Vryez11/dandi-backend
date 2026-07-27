package com.dandi.nyummy.meal.dto

data class UploadImageResponse(
    val uploadUrl: String,
    val imageKey: String,
    val uploadMethod: String,
    val uploadHeaders: Map<String, String>,
    val expiresAt: String,
)
