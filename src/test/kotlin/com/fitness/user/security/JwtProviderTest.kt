package com.fitness.user.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fitness.support.AuthTestSupport.secretOf
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Date

class JwtProviderTest {

    private val clock = Clock.systemUTC()

    @Test
    fun `발급한 access에는 sub·iat·exp 외의 claim이 없다`() {
        // given
        val provider = JwtProvider(JwtProperties(secret = secretOf(32)), clock)

        // when
        val token = provider.issueAccessToken(42L).value

        // then
        val payload = ObjectMapper().readTree(Base64.getUrlDecoder().decode(token.split(".")[1]))
        assertThat(payload.fieldNames().asSequence().toList()).containsExactlyInAnyOrder("sub", "iat", "exp")
    }

    @Test
    fun `서명 키가 32바이트보다 짧으면 생성할 때 실패한다`() {
        // given
        val properties = JwtProperties(secret = secretOf(31))

        // when & then
        assertThrows<IllegalStateException> { JwtProvider(properties, clock) }
    }

    @Test
    fun `HS256이 아닌 알고리즘으로 서명한 토큰은 무효로 판정한다`() {
        // given: HS512도 쓸 수 있을 만큼 긴 키로 같은 키를 쓴 HS512 토큰
        val secret = secretOf(64)
        val provider = JwtProvider(JwtProperties(secret = secret), clock)
        val hs512Token = Jwts.builder()
            .subject("1")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(5))))
            .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)), Jwts.SIG.HS512)
            .compact()

        // when
        val result = provider.parse(hs512Token)

        // then
        assertThat(result).isEqualTo(AccessTokenResult.Invalid)
    }
}
