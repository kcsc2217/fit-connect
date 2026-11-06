package com.fitness.user.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Component

/** 필터 단계(컨트롤러 밖)에서 ProblemDetail을 응답으로 쓴다. 예외 핸들러 응답과 같은 모양이 되도록 앱의 ObjectMapper를 쓴다 */
@Component
class ProblemDetailWriter(private val objectMapper: ObjectMapper) {

    fun write(response: HttpServletResponse, problem: ProblemDetail) {
        response.status = problem.status
        // charset을 붙이지 않는다: JSON은 UTF-8이 표준이고(RFC 8259), writeValue가 UTF-8 바이트로 쓴다.
        // 예외 핸들러 응답의 Content-Type(application/problem+json)과 같게 맞춘다
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        objectMapper.writeValue(response.outputStream, problem)
    }
}
