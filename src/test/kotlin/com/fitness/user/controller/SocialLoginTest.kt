package com.fitness.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fitness.support.AuthTestSupport.userIdOf
import com.fitness.support.FakeOidcProviders
import com.fitness.support.IdTokens
import com.fitness.support.IntegrationTest
import com.fitness.user.dto.LoginResponse
import com.fitness.user.security.JwtProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID

/** 카카오·구글 로그인. 공개키는 가짜 서버(FakeOidcProviders)에서 받는다 */
@IntegrationTest
class SocialLoginTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val jwtProvider: JwtProvider,
) {

    private fun kakaoToken(sub: String = "kakao-" + UUID.randomUUID()) =
        IdTokens.sign(FakeOidcProviders.kakaoKey, sub = sub)

    private fun googleToken(sub: String = "google-" + UUID.randomUUID()) = IdTokens.sign(
        FakeOidcProviders.googleKey, sub = sub, iss = IdTokens.GOOGLE_ISSUER, aud = IdTokens.GOOGLE_AUD,
    )

    private fun login(provider: String, token: String): ResultActionsDsl = mockMvc.post("/api/v1/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"provider": "$provider", "token": "$token"}"""
    }

    private fun loginBody(provider: String, token: String): LoginResponse =
        objectMapper.readValue(login(provider, token).andReturn().response.contentAsString)

    @Test
    fun `카카오 id_token으로 처음 로그인하면 ONBOARDING 회원으로 가입하고 토큰을 발급한다`() {
        // given
        val token = kakaoToken()

        // when
        val result = login("KAKAO", token)

        // then
        result.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("ONBOARDING") }
        }
    }

    @Test
    fun `구글 id_token으로 로그인한 회원의 내 정보에는 GOOGLE 계정이 연결돼 있다`() {
        // given
        val access = loginBody("GOOGLE", googleToken()).accessToken

        // when
        val result = mockMvc.get("/api/v1/users/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $access")
        }

        // then
        result.andExpect {
            status { isOk() }
            jsonPath("$.providers[0]") { value("GOOGLE") }
        }
    }

    @Test
    fun `위조된 카카오 id_token으로 로그인하면 401 INVALID_SOCIAL_TOKEN으로 응답한다`() {
        // given: kid는 같지만 카카오(가짜 서버)의 키가 아닌 다른 키로 서명
        val forged = IdTokens.sign(IdTokens.rsaKey(FakeOidcProviders.kakaoKey.keyID))

        // when
        val result = login("KAKAO", forged)

        // then
        result.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("INVALID_SOCIAL_TOKEN") }
        }
    }

    @Test
    fun `같은 sub라도 카카오와 구글로 로그인하면 서로 다른 회원이 된다`() {
        // given
        val sameSub = "same-" + UUID.randomUUID()
        val kakaoUserId = jwtProvider.userIdOf(loginBody("KAKAO", kakaoToken(sameSub)).accessToken)

        // when
        val googleUserId = jwtProvider.userIdOf(loginBody("GOOGLE", googleToken(sameSub)).accessToken)

        // then
        assertThat(googleUserId).isNotEqualTo(kakaoUserId)
    }
}
