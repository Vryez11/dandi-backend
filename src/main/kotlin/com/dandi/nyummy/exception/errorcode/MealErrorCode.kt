package com.dandi.nyummy.exception.errorcode

import org.springframework.http.HttpStatus

enum class MealErrorCode(override val status: HttpStatus, override val code: String, override val message: String) :
    ErrorCode {
    MEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "api.meal.notFound", "요청한 mealId가 데이터베이스에 존재하지 않습니다."),
}
