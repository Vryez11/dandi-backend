package com.dandi.nyummy.meal.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class UploadImageRequest(
    @field:NotBlank()
    val fileName: String,

    @field:NotBlank()
    val contentType: String,

    @field:NotNull()
    val fileSizeBytes: Long
)
