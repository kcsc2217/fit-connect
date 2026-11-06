package com.fitness.user.security

import com.fitness.common.exception.ErrorCode
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 요청마다 Bearer access를 검증해 SecurityContext에 userId를 넣는다. 예외를 던지지 않는다.
 * 실패 이유는 요청 속성에 남기고, 인증이 필요한 경로면 EntryPoint가 401을 쓴다.
 * 빈으로 등록하지 않는다 (@Component를 붙이면 서블릿 필터로도 자동 등록돼 요청마다 두 번 돈다).
 */
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val token = request.getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.substring(BEARER_PREFIX.length)

        if (token != null) {
            when (val result = jwtProvider.parse(token)) {
                is AccessTokenResult.Valid -> SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken.authenticated(result.userId, null, emptyList())
                AccessTokenResult.Expired -> request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.TOKEN_EXPIRED)
                AccessTokenResult.Invalid -> request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.UNAUTHORIZED)
            }
        }
        chain.doFilter(request, response)
    }

    companion object {
        const val AUTH_ERROR_ATTRIBUTE = "fitness.authError"
        private const val BEARER_PREFIX = "Bearer "
    }
}
