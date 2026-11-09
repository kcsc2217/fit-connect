package com.fitness.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fitness.support.AuthTestSupport.loginAsDev
import com.fitness.support.AuthTestSupport.refreshWith
import com.fitness.support.AuthTestSupport.uniqueDevId
import com.fitness.support.AuthTestSupport.userIdOf
import com.fitness.support.IntegrationTest
import com.fitness.user.security.JwtProperties
import com.fitness.user.security.JwtProvider
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@IntegrationTest
class AuthControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val jwtProvider: JwtProvider,
    private val jwtProperties: JwtProperties,
) {

    @Test
    fun `처음 로그인하면 ONBOARDING 회원으로 가입하고 토큰을 발급한다`() {
        // given
        val body = """{"provider": "DEV", "token": "${uniqueDevId()}"}"""

        // when
        val result = mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

        // then
        result.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("ONBOARDING") }
            jsonPath("$.accessToken") { isNotEmpty() }
            jsonPath("$.accessTokenExpiresAt") { isNotEmpty() }
            jsonPath("$.refreshToken") { isNotEmpty() }
            jsonPath("$.refreshTokenExpiresAt") { isNotEmpty() }
        }
    }

    @Test
    fun `같은 소셜 계정으로 다시 로그인하면 기존 회원으로 로그인한다`() {
        // given
        val devId = uniqueDevId()
        val first = mockMvc.loginAsDev(objectMapper, devId)

        // when
        val second = mockMvc.loginAsDev(objectMapper, devId)

        // then
        assertThat(jwtProvider.userIdOf(second.accessToken)).isEqualTo(jwtProvider.userIdOf(first.accessToken))
    }

    @Test
    fun `확인기가 없는 provider로 로그인하면 400 UNSUPPORTED_PROVIDER로 응답한다`() {
        // given
        // 1-B부터 KAKAO·GOOGLE은 테스트에서 켜져 있다. APPLE은 iOS 출시 때까지 확인기가 없다
        val body = """{"provider": "APPLE", "token": "apple-id-token"}"""

        // when
        val result = mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

        // then
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("UNSUPPORTED_PROVIDER") }
        }
    }

    @Test
    fun `빈 토큰으로 로그인하면 400 INVALID_INPUT으로 응답한다`() {
        // given
        val body = """{"provider": "DEV", "token": ""}"""

        // when
        val result = mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

        // then
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_INPUT") }
            jsonPath("$.errors[0].field") { value("token") }
        }
    }

    @Test
    fun `refresh로 갱신하면 새 access와 다른 refresh를 발급한다`() {
        // given
        val login = mockMvc.loginAsDev(objectMapper)

        // when
        val result = mockMvc.post("/api/v1/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken": "${login.refreshToken}"}"""
        }

        // then
        result.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { isNotEmpty() }
            jsonPath("$.refreshToken") { value(not(login.refreshToken)) }
        }
    }

    @Test
    fun `갱신에 쓴 refresh로 다시 갱신하면 401 INVALID_REFRESH_TOKEN으로 응답한다`() {
        // given
        val login = mockMvc.loginAsDev(objectMapper)
        mockMvc.refreshWith(objectMapper, login.refreshToken)

        // when
        val result = mockMvc.post("/api/v1/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken": "${login.refreshToken}"}"""
        }

        // then
        result.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("INVALID_REFRESH_TOKEN") }
        }
    }

    @Test
    fun `형식이 틀린 refresh로 갱신하면 401 INVALID_REFRESH_TOKEN으로 응답한다`() {
        // given
        val malformed = "not-a-refresh-token"

        // when
        val result = mockMvc.post("/api/v1/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken": "$malformed"}"""
        }

        // then
        result.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("INVALID_REFRESH_TOKEN") }
        }
    }

    @Test
    fun `로그아웃한 refresh로 갱신하면 401 INVALID_REFRESH_TOKEN으로 응답한다`() {
        // given
        val login = mockMvc.loginAsDev(objectMapper)
        mockMvc.post("/api/v1/auth/logout") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken": "${login.refreshToken}"}"""
        }

        // when
        val result = mockMvc.post("/api/v1/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken": "${login.refreshToken}"}"""
        }

        // then
        result.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("INVALID_REFRESH_TOKEN") }
        }
    }

    @Test
    fun `이미 로그아웃한 refresh로 다시 로그아웃해도 204로 응답한다`() {
        // given
        val login = mockMvc.loginAsDev(objectMapper)
        val body = """{"refreshToken": "${login.refreshToken}"}"""
        mockMvc.post("/api/v1/auth/logout") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

        // when
        val result = mockMvc.post("/api/v1/auth/logout") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

        // then
        result.andExpect { status { isNoContent() } }
    }

    @Test
    fun `공개 경로는 만료된 access 헤더가 있어도 처리된다`() {
        // given
        val login = mockMvc.loginAsDev(objectMapper)
        val pastClock = Clock.fixed(Instant.now().minus(Duration.ofMinutes(31)), ZoneOffset.UTC)
        val expiredAccess = JwtProvider(jwtProperties, pastClock).issueAccessToken(1L).value

        // when
        val result = mockMvc.post("/api/v1/auth/refresh") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $expiredAccess")
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken": "${login.refreshToken}"}"""
        }

        // then
        result.andExpect { status { isOk() } }
    }
}
