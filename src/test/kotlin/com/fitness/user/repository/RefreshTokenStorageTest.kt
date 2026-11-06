package com.fitness.user.repository

import com.fitness.support.AuthTestSupport.uniqueDevId
import com.fitness.support.IntegrationTest
import com.fitness.user.domain.AuthProvider
import com.fitness.user.domain.RefreshToken
import com.fitness.user.dto.LoginRequest
import com.fitness.user.service.AuthService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.util.concurrent.TimeUnit

/** Redis 원본 키와 TTL을 직접 읽어 확인한다 (저장소를 거치면 키 형식 오류가 가려진다) */
@IntegrationTest
class RefreshTokenStorageTest @Autowired constructor(
    private val authService: AuthService,
    private val redis: StringRedisTemplate,
) {

    @Test
    fun `로그인하면 refresh가 14일 TTL로 Redis에 저장된다`() {
        // given
        val request = LoginRequest(AuthProvider.DEV, uniqueDevId())

        // when
        val refresh = RefreshToken.parse(authService.login(request).refreshToken)!!

        // then
        val ttl = redis.getExpire("refresh:${refresh.userId}:${refresh.tokenId}", TimeUnit.SECONDS)
        val fourteenDays = Duration.ofDays(14).seconds
        assertThat(ttl).isBetween(fourteenDays - 60, fourteenDays)
    }
}
