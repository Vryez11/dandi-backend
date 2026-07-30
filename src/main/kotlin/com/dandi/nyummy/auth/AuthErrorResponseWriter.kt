package com.dandi.nyummy.auth

import com.dandi.nyummy.exception.ErrorResponse
import com.dandi.nyummy.exception.errorcode.AuthErrorCode
import com.dandi.nyummy.exception.errorcode.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class AuthErrorResponseWriter(private val jsonMapper: JsonMapper) :
    AuthenticationEntryPoint,
    AccessDeniedHandler {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) = write(response, AuthErrorCode.UNAUTHORIZED)

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) = write(response, AuthErrorCode.FORBIDDEN)

    private fun write(response: HttpServletResponse, code: ErrorCode) {
        response.status = code.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        jsonMapper.writeValue(response.writer, ErrorResponse.of(code))
    }
}
