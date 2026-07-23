package com.dandi.nyummy.exception.errorcode

import org.springframework.http.HttpStatus

enum class MealErrorCode(override val status: HttpStatus, override val code: String, override val message: String):ErrorCode {

    UNSUPPORTED_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "api.meal.unsupportedContentType", "허용하지 않는 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "api.meal.fileSizeExceeded", "허용된 최대 파일 용량을 초과했습니다."),
    INVALID_IMAGE_KEY(HttpStatus.BAD_REQUEST, "api.meal.invalidImageKey", "우리 서비스의 S3 Key가 아닙니다."),
    IMAGE_UPLOAD_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "api.meal.imageUploadNotCompleted", "ImageKey에 해당하는 S3 업로드가 완료되지 않았습니다."),
    MEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "api.meal.notFound", "요청한 mealId가 데이터베이스에 존재하지 않습니다.")
}