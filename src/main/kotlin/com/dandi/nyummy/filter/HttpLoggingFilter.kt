package com.dandi.nyummy.filter

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
                log.info("{} {} status={} elapsed={}ms", request.method, request.requestURI, response.status, elapsed)
            }
        }
    }

    // 실패한 health check(503 등)는 장애 단서라 그대로 남긴다.
    private fun isSuccessfulHealthCheck(request: HttpServletRequest, response: HttpServletResponse): Boolean =
        request.requestURI.startsWith(HEALTH_PATH) && response.status in 200..299
}
