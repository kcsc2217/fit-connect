package com.fitness.user.dto

import com.fitness.user.domain.AuthProvider
import com.fitness.user.domain.UserStatus
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class LoginRequest(
    val provider: AuthProvider,
    @field:NotBlank
    val token: String,
)

data class RefreshTokenRequest(
    @field:NotBlank
    val refreshToken: String,
)

data class TokenResponse(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
)

data class LoginResponse(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
    val status: UserStatus,
) {
    companion object {
        fun of(tokens: TokenResponse, status: UserStatus) = LoginResponse(
            tokens.accessToken,
            tokens.accessTokenExpiresAt,
            tokens.refreshToken,
            tokens.refreshTokenExpiresAt,
            status,
        )
    }
}

/** 서비스 간 전달용 (응답 아님) */
data class AccountInfo(val userId: Long, val status: UserStatus)
