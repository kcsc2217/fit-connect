package com.fitness.user.repository

import com.fitness.user.domain.RefreshToken
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

/** refresh 저장소 (Redis, 키 refresh:{userId}:{tokenId}). 서버 메모리에 로그인 상태를 두지 않는다 */
@Repository
class RefreshTokenRepository(
    private val redis: StringRedisTemplate,
) {
    fun save(token: RefreshToken, ttl: Duration) {
        redis.opsForValue().set(key(token), token.userId.toString(), ttl)
    }

    /** 있으면 지우고 true. 같은 토큰으로 동시에 불러도 true는 한 번뿐이다 (Redis DEL은 원자적이고 지운 개수를 돌려준다) */
    fun consume(token: RefreshToken): Boolean = redis.delete(key(token))

    private fun key(token: RefreshToken) = "refresh:${token.userId}:${token.tokenId}"
}
