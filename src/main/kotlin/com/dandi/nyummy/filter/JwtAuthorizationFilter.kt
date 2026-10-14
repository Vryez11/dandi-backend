package com.dandi.nyummy.filter

import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.AuthErrorCode
import com.dandi.nyummy.security.AuthUser
import com.dandi.nyummy.security.jwt.JwtService
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthorizationFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    companion object {
        const val AUTH_EXCEPTION = "authException"

        // 인증된 사용자 ID. HttpLoggingFilter가 요청 로그에 남긴다.
        // 그 필터는 Security 필터 체인 바깥이라 응답 시점에는 SecurityContext가 이미 비워져 있다.
        const val AUTH_USER_ID = "authUserId"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = extractToken(request)

        if (token != null) {
            try {
                val authentication = jwtService.getAuthentication(token)
                SecurityContextHolder.getContextHolderStrategy().context =
                    SecurityContextHolder.createEmptyContext().apply { this.authentication = authentication }
                (authentication.principal as? AuthUser)?.let { request.setAttribute(AUTH_USER_ID, it.userId) }
            } catch (e: ExpiredJwtException) {
                request.setAttribute(AUTH_EXCEPTION, BusinessException(AuthErrorCode.TOKEN_EXPIRED))
            } catch (e: JwtException) {
                request.setAttribute(AUTH_EXCEPTION, BusinessException(AuthErrorCode.UNAUTHORIZED))
            } catch (e: BusinessException) {
                request.setAttribute(AUTH_EXCEPTION, e)
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? = request.getHeader(HttpHeaders.AUTHORIZATION)
        ?.takeIf { it.startsWith("Bearer ") }
        ?.substring(7)
}
