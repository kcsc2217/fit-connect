package com.fitness.common.exception

import com.fitness.support.IntegrationTest
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@IntegrationTest
class GlobalExceptionHandlerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @Test
    fun `비즈니스 예외가 나면 에러 코드의 상태와 code를 담은 ProblemDetail로 응답한다`() {
        // given
        val path = "/test-support/errors/business"

        // when
        val result = mockMvc.get(path)

        // then
        result.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.code") { value("NOT_FOUND") }
            jsonPath("$.detail") { value("종목을 찾을 수 없습니다: 999") }
            jsonPath("$.instance") { value(path) }
        }
    }

    @Test
    fun `요청 본문 검증에 실패하면 400 INVALID_INPUT과 필드별 오류 목록을 응답한다`() {
        // given
        val body = """{"name": "", "count": 0}"""

        // when
        val result = mockMvc.post("/test-support/errors/validation") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

        // then
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_INPUT") }
            jsonPath("$.errors[*].field") { value(containsInAnyOrder("name", "count")) }
        }
    }

    @Test
    fun `필수 필드가 빠진 요청은 400 INVALID_INPUT과 빠진 필드 이름을 응답한다`() {
        // given
        val body = """{"count": 1}"""

        // when
        val result = mockMvc.post("/test-support/errors/validation") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

        // then
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_INPUT") }
            jsonPath("$.errors[0].field") { value("name") }
        }
    }

    @Test
    fun `JSON 문법이 잘못된 요청은 400 INVALID_REQUEST로 응답한다`() {
        // given
        val body = """{"name":"""

        // when
        val result = mockMvc.post("/test-support/errors/validation") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

        // then
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_REQUEST") }
        }
    }

    @Test
    fun `경로 변수 타입이 맞지 않으면 400 INVALID_REQUEST로 응답한다`() {
        // given
        val path = "/test-support/errors/items/abc"

        // when
        val result = mockMvc.get(path)

        // then
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_REQUEST") }
        }
    }

    @Test
    fun `없는 경로를 호출하면 404 NOT_FOUND로 응답한다`() {
        // given
        val path = "/api/v1/nothing"

        // when
        val result = mockMvc.get(path)

        // then
        result.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("NOT_FOUND") }
        }
    }

    @Test
    fun `지원하지 않는 HTTP 메서드로 호출하면 405 METHOD_NOT_ALLOWED로 응답한다`() {
        // given
        val path = "/test-support/errors/business"

        // when
        val result = mockMvc.delete(path)

        // then
        result.andExpect {
            status { isMethodNotAllowed() }
            jsonPath("$.code") { value("METHOD_NOT_ALLOWED") }
        }
    }

    @Test
    fun `예상하지 못한 예외가 나면 500 INTERNAL_ERROR로 응답하고 내부 메시지를 노출하지 않는다`() {
        // given
        val path = "/test-support/errors/unexpected"

        // when
        val result = mockMvc.get(path)

        // then
        result.andExpect {
            status { isInternalServerError() }
            jsonPath("$.code") { value("INTERNAL_ERROR") }
            content { string(not(containsString("users 테이블"))) }
        }
    }

    @Test
    fun `원시 타입 필드가 빠진 요청은 0으로 채우지 않고 400 INVALID_INPUT과 빠진 필드 이름을 응답한다`() {
        // given
        val body = """{}"""

        // when
        val result = mockMvc.post("/test-support/errors/primitive") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

        // then
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_INPUT") }
            jsonPath("$.errors[0].field") { value("reps") }
        }
    }

    @Test
    fun `JSON 필드 타입이 맞지 않으면 400 INVALID_INPUT과 해당 필드 이름을 응답한다`() {
        // given
        val body = """{"name": "a", "count": "abc"}"""

        // when
        val result = mockMvc.post("/test-support/errors/validation") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

        // then
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_INPUT") }
            jsonPath("$.errors[0].field") { value("count") }
        }
    }

    @Test
    fun `중첩 리스트의 필드가 빠지면 검증 실패와 같은 형식의 필드 경로로 응답한다`() {
        // given
        val body = """{"items": [{"count": 1}]}"""

        // when
        val result = mockMvc.post("/test-support/errors/nested") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

        // then
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0].field") { value("items[0].name") }
        }
    }

    @Test
    fun `리스트 본문 검증에 실패하면 400 INVALID_INPUT과 원소 위치가 담긴 필드 경로를 응답한다`() {
        // given
        val body = """[{"name": "a", "count": 1}, {"name": "", "count": 1}]"""

        // when
        val result = mockMvc.post("/test-support/errors/validation-list") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

        // then
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_INPUT") }
            jsonPath("$.errors[0].field") { value("[1].name") }
        }
    }

    @Test
    fun `쿼리 파라미터 제약에 어긋나면 400 INVALID_INPUT과 파라미터 이름을 응답한다`() {
        // given
        val path = "/test-support/errors/page?page=0"

        // when
        val result = mockMvc.get(path)

        // then
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_INPUT") }
            jsonPath("$.errors[0].field") { value("page") }
        }
    }

    @Test
    fun `Spring이 처리하는 5xx 예외는 500 INTERNAL_ERROR로 응답하고 내부 문구를 숨긴다`() {
        // given
        val path = "/test-support/errors/missing-path-variable"

        // when
        val result = mockMvc.get(path)

        // then
        result.andExpect {
            status { isInternalServerError() }
            jsonPath("$.code") { value("INTERNAL_ERROR") }
            jsonPath("$.detail") { value("서버 오류가 발생했습니다") }
        }
    }
}
