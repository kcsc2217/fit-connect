package com.fitness.user.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException

class ProblemDetailAccessDeniedHandlerTest {

    // Spring Boot와 같은 방식(빌더)으로 만든 ObjectMapper: ProblemDetail의 code가 최상위 필드로 나온다
    private val objectMapper = Jackson2ObjectMapperBuilder.json().build<com.fasterxml.jackson.databind.ObjectMapper>()
    private val handler = ProblemDetailAccessDeniedHandler(ProblemDetailWriter(objectMapper))

    @Test
    fun `권한이 없으면 403 FORBIDDEN ProblemDetail을 쓴다`() {
        // given
        val request = MockHttpServletRequest("GET", "/api/v1/admin")
        val response = MockHttpServletResponse()

        // when
        handler.handle(request, response, AccessDeniedException("권한 없음"))

        // then
        val body = objectMapper.readTree(response.contentAsByteArray)
        assertThat(response.status to body["code"].asText()).isEqualTo(403 to "FORBIDDEN")
    }
}
