@file:Suppress("ktlint:standard:filename")

package com.dandi.nyummy.user.dto

import com.dandi.nyummy.user.enum.Gender
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class UserResponse(
    val id: Long,
    val email: String?,
    val nickname: String?,
    val birth: LocalDate?,
    val gender: Gender?,
    val height: Int?,
    val weight: Int?,
    val coin: Int,
    val breakfastHour: Int?,
    val lunchHour: Int?,
    val dinnerHour: Int?,
)

data class HomeUser(val coin: Int)

data class PasswordUpdateRequest(
    @field:NotBlank
    val currentPassword: String,

    @field:NotBlank
    @field:Size(min = 8, max = 64)
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$",
        message = "비밀번호는 영문과 숫자를 모두 포함해야 합니다.",
    )
    val newPassword: String,

    @field:NotBlank
    val confirmNewPassword: String,
) {
    @get:AssertTrue(message = "새 비밀번호가 일치하지 않습니다.")
    val isPasswordConfirmed: Boolean
        get() = newPassword == confirmNewPassword
}

data class PasswordUpdateResponse(val accessToken: String, val refreshToken: String)
