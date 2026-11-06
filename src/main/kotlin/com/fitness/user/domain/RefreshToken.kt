package com.fitness.user.domain

import java.security.SecureRandom
import java.util.Base64

/**
 * refresh 값 "{userId}.{tokenId}" (docs/decisions/0004-auth-token.md).
 * tokenId는 32바이트 랜덤(Base64url 43자)이라 추측할 수 없다. Redis 키는 refresh:{userId}:{tokenId}.
 * data class가 아니다: 자동 toString이 tokenId를 로그에 남기지 않게 한다.
 */
class RefreshToken private constructor(
    val userId: Long,
    val tokenId: String,
) {
    val value: String get() = "$userId.$tokenId"

    override fun toString() = "RefreshToken(userId=$userId, tokenId=***)"

    companion object {
        private const val TOKEN_BYTES = 32
        private val random = SecureRandom()
        private val encoder = Base64.getUrlEncoder().withoutPadding()
        private val TOKEN_ID = Regex("^[A-Za-z0-9_-]{43}$")

        fun issue(userId: Long): RefreshToken {
            val bytes = ByteArray(TOKEN_BYTES).also { random.nextBytes(it) }
            return RefreshToken(userId, encoder.encodeToString(bytes))
        }

        /** 형식이 틀리면 null. Redis를 조회하기 전에 걸러낸다 */
        fun parse(value: String): RefreshToken? {
            val parts = value.split(".")
            if (parts.size != 2) return null
            val userId = parts[0].toLongOrNull() ?: return null
            if (!TOKEN_ID.matches(parts[1])) return null
            return RefreshToken(userId, parts[1])
        }
    }
}
