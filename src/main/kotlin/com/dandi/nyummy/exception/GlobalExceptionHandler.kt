package com.dandi.nyummy.exception

import com.dandi.nyummy.exception.errorcode.CommonErrorCode
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class GlobalExceptionHandler(private val meterRegistry: MeterRegistry) : ResponseEntityExceptionHandler() {

    companion object {
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

        // 에러 코드별 발생 횟수. 같은 상태 코드(409 등)를 쓰는 에러를 구분해 센다.
        // Security 단계의 401·403도 SecurityExceptionHandler가 이 핸들러로 넘기므로 함께 집계된다.
        private const val BUSINESS_EXCEPTION_METRIC_NAME = "nyummy.business.exceptions"
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponse> {
        log.warn("BusinessException: code={}, message={}", e.errorCode.code, e.message)
        // code는 ErrorCode enum 값이라 종류가 정해져 있어 태그로 써도 series가 늘지 않는다.
        meterRegistry.counter(BUSINESS_EXCEPTION_METRIC_NAME, "code", e.errorCode.code).increment()
        return ResponseEntity
            .status(e.errorCode.status)
            .body(ErrorResponse.of(e.errorCode.code, e.message ?: e.errorCode.message))
    }

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errorCode = convertErrorCode(statusCode)
        if (statusCode.is5xxServerError) {
            log.error("Spring MVC exception: status={}", statusCode.value(), ex)
        } else {
            log.warn("Spring MVC exception: status={}, message={}", statusCode.value(), ex.message)
        }

        // 입력 오류만 원인 파악용으로 Spring 메시지를 내보내고, 나머지는 내부 문구를 숨긴다
        val message = when (errorCode) {
            CommonErrorCode.INVALID_INPUT_VALUE -> ex.message ?: errorCode.message
            else -> errorCode.message
        }
        return ResponseEntity
            .status(statusCode)
            .body(ErrorResponse.of(errorCode.code, message))
    }

    // 상태코드는 Spring이 정한 값을 그대로 쓰고, 응답 code만 상태에 맞춰 고른다
    private fun convertErrorCode(statusCode: HttpStatusCode): CommonErrorCode = when {
        statusCode.isSameCodeAs(HttpStatus.NOT_FOUND) -> CommonErrorCode.NOT_FOUND
        statusCode.is5xxServerError -> CommonErrorCode.INTERNAL_SERVER_ERROR
        else -> CommonErrorCode.INVALID_INPUT_VALUE
    }

    /*
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        e: MethodArgumentNotValidException,
    ): ResponseEntity<ErrorResponse> {
        val message = e.bindingResult.fieldErrors.joinToString(", ") {
            fieldError ->
            "${fieldError.field}: ${fieldError.defaultMessage}"
        }
        log.warn("MethodArgumentNotValidException: {}", message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of("api.common.invalidFormat", message))
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(
        e: MethodArgumentTypeMismatchException,
    ): ResponseEntity<ErrorResponse> {
        val message = e.message
        log.warn("MethodArgumentTypeMismatchException: {}", message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of("api.common.invalidFormat", message))
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(
        e: MissingServletRequestParameterException,
    ): ResponseEntity<ErrorResponse> {
        val message = e.message
        log.warn("MissingServletRequestParameterException: {}", message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of("api.common.missingParameter", message))
    }
     */

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR))
    }
}
