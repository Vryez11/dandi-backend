package com.dandi.nyummy.security.filter

import com.dandi.nyummy.security.AuthUser
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class StubAuthenticationFilter : OncePerRequestFilter() {

    companion object {
        const val USER_ID_HEADER = "X-User-Id"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val userId = request.getHeader(USER_ID_HEADER)?.toLongOrNull()
        if (userId != null) {
            val authentication = UsernamePasswordAuthenticationToken.authenticated(
                AuthUser(userId),
                null,
                listOf(SimpleGrantedAuthority("ROLE_USER")),
            )
            SecurityContextHolder.setContext(
                SecurityContextHolder.createEmptyContext().apply { this.authentication = authentication },
            )
        }

        filterChain.doFilter(request, response)
    }
}
