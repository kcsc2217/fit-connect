package com.fitness.user.security

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.Date
import javax.crypto.SecretKey

/** access 발급·검증 (docs/decisions/0004-auth-token.md). HS256 고정, claim은 sub·iat·exp만 */
@Component
class JwtProvider(
    private val properties: JwtProperties,
    private val clock: Clock,
) {
    // 시작할 때 키를 검증한다: 비었거나 짧으면 앱이 뜨지 않는다 (운영에서 JWT_SECRET 누락을 바로 알게)
    private val key: SecretKey = run {
        val bytes = try {
            Decoders.BASE64.decode(properties.secret)
        } catch (e: RuntimeException) {
            throw IllegalStateException("fitness.jwt.secret은 Base64 값이어야 합니다")
        }
        check(bytes.size >= MIN_KEY_BYTES) { "fitness.jwt.secret은 디코드 후 32바이트 이상이어야 합니다 (현재 ${bytes.size})" }
        Keys.hmacShaKeyFor(bytes)
    }

    private val parser: JwtParser = Jwts.parser()
        .verifyWith(key)
        .clockSkewSeconds(CLOCK_SKEW_SECONDS)
        .clock { Date.from(clock.instant()) }
        .build()

    fun issueAccessToken(userId: Long): IssuedToken {
        val now = clock.instant()
        val expiresAt = now.plus(properties.accessTtl)
        val token = Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(key, Jwts.SIG.HS256)
            .compact()
        return IssuedToken(token, expiresAt)
    }

    fun parse(token: String): AccessTokenResult = try {
        val jws = parser.parseSignedClaims(token)
        val userId = jws.payload.subject?.toLongOrNull()
        when {
            // 헤더의 alg를 믿지 않는다: 같은 키로 HS512 서명한 토큰도 거절
            jws.header.algorithm != Jwts.SIG.HS256.id -> AccessTokenResult.Invalid
            userId == null -> AccessTokenResult.Invalid
            else -> AccessTokenResult.Valid(userId)
        }
    } catch (e: ExpiredJwtException) {
        AccessTokenResult.Expired
    } catch (e: JwtException) { // 서명 불일치, 형식 오류, alg=none(서명 없는 토큰) 등
        AccessTokenResult.Invalid
    } catch (e: IllegalArgumentException) { // 빈 문자열
        AccessTokenResult.Invalid
    }

    companion object {
        private const val MIN_KEY_BYTES = 32
        private const val CLOCK_SKEW_SECONDS = 5L
    }
}
