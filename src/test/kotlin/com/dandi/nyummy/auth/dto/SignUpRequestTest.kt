package com.dandi.nyummy.auth.dto

import com.dandi.nyummy.profile.enum.Gender
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertTrue

class SignUpRequestTest {

    private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

    private fun createRequest(
        emailVerifiedToken: String = "token",
        password: String = "password1",
        confirmPassword: String = "password1",
        nickname: String = "냠미",
        gender: Gender? = Gender.MALE,
        birth: LocalDate? = LocalDate.of(2000, 1, 1),
        height: Int? = 170,
        weight: Int? = 60,
    ) = SignUpRequest(emailVerifiedToken, password, confirmPassword, nickname, gender, birth, height, weight)

    private fun validate(request: SignUpRequest) = validator.validate(request)

    private fun assertViolationOn(request: SignUpRequest, propertyName: String) {
        val violations = validate(request)
        assertTrue(
            violations.any { it.propertyPath.toString() == propertyName },
            "expected violation on '$propertyName' but got: $violations",
        )
    }

    @Test
    fun `정상 요청은 위반이 없다`() {
        assertTrue(validate(createRequest()).isEmpty())
    }

    @Test
    fun `비밀번호가 8자 미만이면 위반이다`() {
        assertViolationOn(createRequest(password = "pass1", confirmPassword = "pass1"), "password")
    }

    @Test
    fun `비밀번호가 64자를 초과하면 위반이다`() {
        val longPassword = "a1".repeat(33)
        assertViolationOn(createRequest(password = longPassword, confirmPassword = longPassword), "password")
    }

    @Test
    fun `비밀번호에 숫자가 없으면 위반이다`() {
        assertViolationOn(createRequest(password = "passwordonly", confirmPassword = "passwordonly"), "password")
    }

    @Test
    fun `비밀번호에 영문이 없으면 위반이다`() {
        assertViolationOn(createRequest(password = "12345678", confirmPassword = "12345678"), "password")
    }

    @Test
    fun `비밀번호 재입력이 일치하지 않으면 위반이다`() {
        assertViolationOn(createRequest(confirmPassword = "different1"), "passwordConfirmed")
    }

    @Test
    fun `닉네임이 공백이면 위반이다`() {
        assertViolationOn(createRequest(nickname = " "), "nickname")
    }

    @Test
    fun `신체 정보가 전부 없어도 위반이 아니다`() {
        val request = createRequest(gender = null, birth = null, height = null, weight = null)
        assertTrue(validate(request).isEmpty())
    }

    @Test
    fun `키가 음수이면 위반이다`() {
        assertViolationOn(createRequest(height = -1), "height")
    }

    @Test
    fun `몸무게가 음수이면 위반이다`() {
        assertViolationOn(createRequest(weight = -1), "weight")
    }

    @Test
    fun `생일이 미래 날짜이면 위반이다`() {
        assertViolationOn(createRequest(birth = LocalDate.now().plusDays(1)), "birth")
    }
}
