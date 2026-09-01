package com.dandi.nyummy.auth.dto

import com.dandi.nyummy.profile.enum.Gender
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class SignUpRequest(

    @field:NotBlank
    val emailVerifiedToken: String,

    @field:NotBlank
    @field:Size(min = 8, max = 64)
    @field:Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "비밀번호는 영문과 숫자를 모두 포함해야 합니다.")
    val password: String,

    @field:NotBlank
    val confirmPassword: String,

    @field:NotBlank
    @field:Size(max = 100)
    val nickname: String,

    val gender: Gender? = null,

    @field:Past
    val birth: LocalDate? = null,

    @field:Positive
    val height: Int? = null,

    @field:Positive
    val weight: Int? = null,
) {
    @get:AssertTrue(message = "비밀번호가 일치하지 않습니다.")
    val isPasswordConfirmed: Boolean
        get() = password == confirmPassword
}
