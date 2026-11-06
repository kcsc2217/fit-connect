package com.fitness.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fitness.support.AuthTestSupport.loginAsDev
import com.fitness.support.AuthTestSupport.secretOf
import com.fitness.support.IntegrationTest
import com.fitness.user.security.JwtProperties
import com.fitness.user.security.JwtProvider
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64

@IntegrationTest
class UserControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val jwtProperties: JwtProperties,
) {

    @Test
    fun `로그인해서 받은 access로 내 정보를 조회한다`() {
        // given
        val login = mockMvc.loginAsDev(objectMapper)

        // when
        val result = mockMvc.get("/api/v1/users/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer ${login.accessToken}")
        }

        // then
        result.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("ONBOARDING") }
            jsonPath("$.providers[0]") { value("DEV") }
        }
    }

    @Test
    fun `토큰 없이 내 정보를 조회하면 401 UNAUTHORIZED ProblemDetail로 응답한다`() {
        // given
        val path = "/api/v1/users/me"

        // when
        val result = mockMvc.get(path)

        // then
        result.andExpect {
            status { isUnauthorized() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            header { string(HttpHeaders.WWW_AUTHENTICATE, "Bearer") }
            jsonPath("$.code") { value("UNAUTHORIZED") }
            jsonPath("$.instance") { value(path) }
        }
    }

    @Test
    fun `다른 키로 서명한 access로 내 정보를 조회하면 401 UNAUTHORIZED로 응답한다`() {
        // given
        val otherProvider = JwtProvider(JwtProperties(secret = secretOf(32, seed = 7)), Clock.systemUTC())
        val forged = otherProvider.issueAccessToken(1L).value

        // when
        val result = mockMvc.get("/api/v1/users/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $forged")
        }

        // then
        result.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("UNAUTHORIZED") }
        }
    }

    @Test
    fun `만료된 access로 내 정보를 조회하면 401 TOKEN_EXPIRED로 응답한다`() {
        // given
        val pastClock = Clock.fixed(Instant.now().minus(Duration.ofMinutes(31)), ZoneOffset.UTC)
        val expired = JwtProvider(jwtProperties, pastClock).issueAccessToken(1L).value

        // when
        val result = mockMvc.get("/api/v1/users/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $expired")
        }

        // then
        result.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("TOKEN_EXPIRED") }
        }
    }

    @Test
    fun `서명 없는 토큰으로 내 정보를 조회하면 401 UNAUTHORIZED로 응답한다`() {
        // given: alg=none, 서명 부분이 빈 토큰
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"none"}""".toByteArray())
        val exp = Instant.now().plus(Duration.ofMinutes(10)).epochSecond
        val payload = encoder.encodeToString("""{"sub":"1","exp":$exp}""".toByteArray())
        val unsigned = "$header.$payload."

        // when
        val result = mockMvc.get("/api/v1/users/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $unsigned")
        }

        // then
        result.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("UNAUTHORIZED") }
        }
    }

    @Test
    fun `인증 없이 없는 경로를 호출하면 404가 아니라 401로 응답한다`() {
        // given
        val path = "/api/v1/nothing"

        // when
        val result = mockMvc.get(path)

        // then
        result.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("UNAUTHORIZED") }
        }
    }
}
