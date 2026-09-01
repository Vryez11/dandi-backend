package com.dandi.nyummy.auth.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class ConfirmAuthCodeRequest(

    @field:NotBlank
    @field:Pattern(regexp = "^\\d{6}$")
    val authCode: String,

    @field:NotBlank
    val emailChallengeToken: String,
)
