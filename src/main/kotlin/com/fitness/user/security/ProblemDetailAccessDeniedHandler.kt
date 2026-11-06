package com.fitness.user.security

import com.fitness.common.exception.ErrorCode
import com.fitness.common.exception.problemDetail
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

/** 인증은 됐지만 권한이 없을 때 403. 1-A에는 권한 구분이 없어 형식만 맞춰 둔다 */
@Component
class ProblemDetailAccessDeniedHandler(private val writer: ProblemDetailWriter) : AccessDeniedHandler {

    override fun handle(request: HttpServletRequest, response: HttpServletResponse, e: AccessDeniedException) {
        writer.write(response, problemDetail(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.title, request.requestURI))
    }
}
