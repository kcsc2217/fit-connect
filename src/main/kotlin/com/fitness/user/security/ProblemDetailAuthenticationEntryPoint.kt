package com.fitness.user.security

import com.fitness.common.exception.ErrorCode
import com.fitness.common.exception.problemDetail
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

/** 인증이 필요한 경로에 인증이 없을 때 401. 만료인지 무효인지는 필터가 남긴 요청 속성으로 구분한다 */
@Component
class ProblemDetailAuthenticationEntryPoint(
    private val writer: ProblemDetailWriter,
) : AuthenticationEntryPoint {

    override fun commence(request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
        val code = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE) as? ErrorCode
            ?: ErrorCode.UNAUTHORIZED
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        writer.write(response, problemDetail(code, code.title, request.requestURI))
    }
}
