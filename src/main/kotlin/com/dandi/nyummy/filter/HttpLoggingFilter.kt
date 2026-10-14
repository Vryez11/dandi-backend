package com.dandi.nyummy.filter

import com.dandi.nyummy.header.toCommonHeader
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class HttpLoggingFilter : OncePerRequestFilter() {

    companion object {
        private val log = LoggerFactory.getLogger(HttpLoggingFilter::class.java)

        // ALB·compose가 짧은 주기로 호출한다. 성공 응답까지 남기면 로그 대부분이 health check로 채워진다.
        private const val HEALTH_PATH = "/actuator/health"

        // 토큰이 없거나 인증에 실패한 요청
        private const val ANONYMOUS_USER_ID = "anonymous"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val startTime = System.nanoTime()
        try {
            filterChain.doFilter(request, response)
        } finally {
            val elapsed = (System.nanoTime() - startTime) / 1_000_000
            if (!isSuccessfulHealthCheck(request, response)) {
                // 403이 소유권 위반(로그인 사용자)인지 막힌 경로 접근(대부분 비로그인)인지 userId로 가른다.
                // userId는 메트릭 태그로 쓰면 사용자 수만큼 series가 생기므로 로그에만 남긴다.
                log.info(
                    "{} {} status={} elapsed={}ms userId={} {}",
                    request.method,
                    request.requestURI,
                    response.status,
                    elapsed,
                    request.getAttribute(JwtAuthorizationFilter.AUTH_USER_ID) ?: ANONYMOUS_USER_ID,
                    request.toCommonHeader(),
                )
            }
        }
    }

    // 실패한 health check(503 등)는 장애 단서라 그대로 남긴다.
    private fun isSuccessfulHealthCheck(request: HttpServletRequest, response: HttpServletResponse): Boolean =
        request.requestURI.startsWith(HEALTH_PATH) && response.status in 200..299
}
