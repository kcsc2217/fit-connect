package com.fitness.user.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/** 토큰 설정 (docs/decisions/0004-auth-token.md). secret은 Base64, 디코드 후 32바이트 이상 */
@ConfigurationProperties("fitness.jwt")
data class JwtProperties(
    val secret: String,
    val accessTtl: Duration = Duration.ofMinutes(30),
    val refreshTtl: Duration = Duration.ofDays(14),
)
