package com.dandi.nyummy.meal.dto

import jakarta.validation.constraints.NotBlank

data class UploadImageRequest(
    @field:NotBlank
    val fileName: String,

    @field:NotBlank
    val contentType: String,

    @field:NotBlank
    val fileSizeBytes: Long
)
