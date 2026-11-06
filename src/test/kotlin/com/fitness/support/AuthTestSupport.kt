package com.fitness.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fitness.user.dto.LoginResponse
import com.fitness.user.dto.TokenResponse
import com.fitness.user.security.AccessTokenResult
import com.fitness.user.security.JwtProvider
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.Base64
import java.util.UUID

/**
 * 인증 API 테스트 도우미.
 * 통합 테스트는 롤백하지 않으므로(계획 006) 테스트마다 고유한 개발용 소셜 ID로 회원을 만든다.
 */
object AuthTestSupport {

    fun uniqueDevId(): String = "dev-" + UUID.randomUUID()

    /** 테스트용 서명 키: n바이트를 Base64로 */
    fun secretOf(bytes: Int, seed: Int = 0): String =
        Base64.getEncoder().encodeToString(ByteArray(bytes) { (it + seed).toByte() })

    fun MockMvc.loginAsDev(objectMapper: ObjectMapper, devId: String = uniqueDevId()): LoginResponse {
        val body = post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"provider": "DEV", "token": "$devId"}"""
        }.andReturn().response.contentAsString
        return objectMapper.readValue(body)
    }

    fun MockMvc.refreshWith(objectMapper: ObjectMapper, refreshToken: String): TokenResponse {
        val body = post("/api/v1/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken": "$refreshToken"}"""
        }.andReturn().response.contentAsString
        return objectMapper.readValue(body)
    }

    fun JwtProvider.userIdOf(accessToken: String): Long =
        (parse(accessToken) as AccessTokenResult.Valid).userId
}
