package com.fitness.user.client

import com.fitness.common.exception.BusinessException
import com.fitness.common.exception.ErrorCode
import com.fitness.support.FakeJwksServer
import com.fitness.support.IdTokens
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant

/** 가짜 공개키 서버 + 실제 Nimbus decoder로 검증 경로를 끝까지 탄다 (Spring 없이) */
class OidcIdTokenVerifierTest {

    private val server = FakeJwksServer()
    private val key = IdTokens.rsaKey()

    @AfterEach
    fun tearDown() = server.close()

    private fun kakaoSettings(jwkSetUri: String = server.uri) = OidcProviderSettings.kakao(
        SocialLoginProperties.Provider(audiences = listOf(IdTokens.KAKAO_AUD), jwkSetUri = jwkSetUri),
    )

    private fun googleSettings() = OidcProviderSettings.google(
        SocialLoginProperties.Provider(audiences = listOf(IdTokens.GOOGLE_AUD), jwkSetUri = server.uri),
    )

    private fun verifier(settings: OidcProviderSettings = kakaoSettings()) =
        OidcIdTokenVerifier(settings.provider, OidcJwtDecoders.create(settings))

    private fun errorCodeOf(verifier: OidcIdTokenVerifier, token: String): ErrorCode =
        assertThrows<BusinessException> { verifier.verify(token) }.errorCode

    @Test
    fun `카카오가 우리 앱에 발급한 유효한 id_token이면 sub로 신원을 돌려준다`() {
        // given
        server.keys = listOf(key)
        val token = IdTokens.sign(key, sub = "3456789012")

        // when
        val identity = verifier().verify(token)

        // then
        assertThat(identity).isEqualTo(SocialIdentity(com.fitness.user.domain.AuthProvider.KAKAO, "3456789012", null))
    }

    @Test
    fun `다른 키로 서명한 토큰이면 INVALID_SOCIAL_TOKEN 예외를 던진다`() {
        // given: 서버에 올린 키와 kid는 같지만 다른 비밀키로 서명
        server.keys = listOf(key)
        val forged = IdTokens.sign(IdTokens.rsaKey(key.keyID))

        // when
        val code = errorCodeOf(verifier(), forged)

        // then
        assertThat(code).isEqualTo(ErrorCode.INVALID_SOCIAL_TOKEN)
    }

    @Test
    fun `HS256으로 서명한 토큰이면 INVALID_SOCIAL_TOKEN 예외를 던진다`() {
        // given: 공개키 바이트를 HMAC 비밀키로 쓴 알고리즘 혼동 공격
        server.keys = listOf(key)
        val confused = IdTokens.signHs256(key.toRSAPublicKey().encoded, key.keyID)

        // when
        val code = errorCodeOf(verifier(), confused)

        // then
        assertThat(code).isEqualTo(ErrorCode.INVALID_SOCIAL_TOKEN)
    }

    @Test
    fun `발급자가 카카오가 아닌 토큰이면 INVALID_SOCIAL_TOKEN 예외를 던진다`() {
        // given
        server.keys = listOf(key)
        val googleIssued = IdTokens.sign(key, iss = IdTokens.GOOGLE_ISSUER)

        // when
        val code = errorCodeOf(verifier(), googleIssued)

        // then
        assertThat(code).isEqualTo(ErrorCode.INVALID_SOCIAL_TOKEN)
    }

    @Test
    fun `허용 목록에 없는 앱의 토큰이면 INVALID_SOCIAL_TOKEN 예외를 던진다`() {
        // given
        server.keys = listOf(key)
        val otherApp = IdTokens.sign(key, aud = "other-app-key")

        // when
        val code = errorCodeOf(verifier(), otherApp)

        // then
        assertThat(code).isEqualTo(ErrorCode.INVALID_SOCIAL_TOKEN)
    }

    @Test
    fun `만료된 토큰이면 INVALID_SOCIAL_TOKEN 예외를 던진다`() {
        // given: 허용 오차(30초)보다 한참 전에 만료
        server.keys = listOf(key)
        val expired = IdTokens.sign(key, exp = Instant.now().minus(Duration.ofHours(1)))

        // when
        val code = errorCodeOf(verifier(), expired)

        // then
        assertThat(code).isEqualTo(ErrorCode.INVALID_SOCIAL_TOKEN)
    }

    @Test
    fun `exp가 없는 토큰이면 INVALID_SOCIAL_TOKEN 예외를 던진다`() {
        // given
        server.keys = listOf(key)
        val noExp = IdTokens.sign(key, exp = null)

        // when
        val code = errorCodeOf(verifier(), noExp)

        // then
        assertThat(code).isEqualTo(ErrorCode.INVALID_SOCIAL_TOKEN)
    }

    @Test
    fun `sub가 100자를 넘으면 INVALID_SOCIAL_TOKEN 예외를 던진다`() {
        // given
        server.keys = listOf(key)
        val longSub = IdTokens.sign(key, sub = "a".repeat(101))

        // when
        val code = errorCodeOf(verifier(), longSub)

        // then
        assertThat(code).isEqualTo(ErrorCode.INVALID_SOCIAL_TOKEN)
    }

    @Test
    fun `구글 확인기는 https 없는 발급자 accounts google com도 받는다`() {
        // given
        server.keys = listOf(key)
        val token = IdTokens.sign(key, sub = "google-sub", iss = "accounts.google.com", aud = IdTokens.GOOGLE_AUD)

        // when
        val identity = verifier(googleSettings()).verify(token)

        // then
        assertThat(identity.providerUserId).isEqualTo("google-sub")
    }

    @Test
    fun `공개키 서버에 연결할 수 없으면 SOCIAL_LOGIN_UNAVAILABLE 예외를 던진다`() {
        // given: 주소만 받아 두고 서버를 내린다
        val closed = FakeJwksServer().also { it.close() }
        val verifier = verifier(kakaoSettings(jwkSetUri = closed.uri))

        // when
        val code = errorCodeOf(verifier, IdTokens.sign(key))

        // then
        assertThat(code).isEqualTo(ErrorCode.SOCIAL_LOGIN_UNAVAILABLE)
    }

    @Test
    fun `공개키 서버가 응답하지 않으면 몇 초 안에 SOCIAL_LOGIN_UNAVAILABLE 예외를 던진다`() {
        // given: 읽기 타임아웃(1초)보다 훨씬 늦게 응답
        server.keys = listOf(key)
        server.delay = Duration.ofSeconds(5)
        val verifier = verifier()
        val started = System.nanoTime()

        // when
        val code = errorCodeOf(verifier, IdTokens.sign(key))

        // then: 타임아웃 1초 + 재시도 1회라 5초 지연을 기다리지 않는다
        val elapsed = Duration.ofNanos(System.nanoTime() - started)
        assertThat(code to (elapsed < Duration.ofSeconds(4))).isEqualTo(ErrorCode.SOCIAL_LOGIN_UNAVAILABLE to true)
    }

    @Test
    fun `키가 교체돼 새 kid로 서명한 토큰이 오면 공개키를 다시 받아 검증한다`() {
        // given: 옛 키로 한 번 검증해 캐시를 채운 뒤 회사가 키를 교체
        server.keys = listOf(key)
        val verifier = verifier()
        verifier.verify(IdTokens.sign(key))
        val newKey = IdTokens.rsaKey()
        server.keys = listOf(newKey)

        // when
        val identity = verifier.verify(IdTokens.sign(newKey, sub = "after-rotation"))

        // then
        assertThat(identity.providerUserId).isEqualTo("after-rotation")
    }

    @Test
    fun `없는 kid로 서명한 토큰이 연달아 와도 공개키 서버를 반복해서 부르지 않는다`() {
        // given
        server.keys = listOf(key)
        val verifier = verifier()
        val unknownKidTokens = (1..5).map { IdTokens.sign(IdTokens.rsaKey()) } // 서버에 없는 kid 5개

        // when
        unknownKidTokens.forEach { runCatching { verifier.verify(it) } }

        // then: 처음 받기 1번 + kid가 없어 다시 받기 1번. 나머지는 30초 제한에 걸린다 (0번이면 검증이 키 조회 전에 끝난 것이라 같음으로 확인)
        assertThat(server.requestCount.get()).isEqualTo(2)
    }

    @Test
    fun `다시 받기 제한에 걸린 없는 kid 토큰이면 INVALID_SOCIAL_TOKEN 예외를 던진다`() {
        // given: 없는 kid 토큰 두 건으로 30초 안의 받기 2번(처음 + 다시 받기)을 다 쓴다
        server.keys = listOf(key)
        val verifier = verifier()
        repeat(2) { runCatching { verifier.verify(IdTokens.sign(IdTokens.rsaKey())) } }

        // when: 공개키 서버는 정상이므로 제한에 걸린 것은 장애가 아니라 토큰 문제다
        val code = errorCodeOf(verifier, IdTokens.sign(IdTokens.rsaKey()))

        // then
        assertThat(code).isEqualTo(ErrorCode.INVALID_SOCIAL_TOKEN)
    }

    @Test
    fun `공개키를 받은 적 없이 공개키 서버 장애가 이어지면 다시 받기 제한에 걸려도 SOCIAL_LOGIN_UNAVAILABLE 예외를 던진다`() {
        // given: 실패한 받기도 제한 횟수로 세므로 두 번 실패한 뒤 세 번째는 제한에 걸린다 (리뷰 지적, 실행 재현)
        val closed = FakeJwksServer().also { it.close() }
        val verifier = verifier(kakaoSettings(jwkSetUri = closed.uri))
        repeat(2) { runCatching { verifier.verify(IdTokens.sign(key)) } }

        // when
        val code = errorCodeOf(verifier, IdTokens.sign(key))

        // then
        assertThat(code).isEqualTo(ErrorCode.SOCIAL_LOGIN_UNAVAILABLE)
    }

    @Test
    fun `공개키를 받은 뒤 공개키 서버가 장애여도 받아 둔 키로 검증한다`() {
        // given: 캐시를 짧게 두고 한 번 받은 뒤, 캐시가 만료된 상태에서 서버 장애
        // (Nimbus는 다시 받기 제한 간격이 캐시 수명보다 짧아야 해서 둘 다 줄인다)
        server.keys = listOf(key)
        val shortCache = kakaoSettings().copy(cacheTtl = Duration.ofMillis(200), refetchMinInterval = Duration.ofMillis(100))
        val verifier = verifier(shortCache)
        verifier.verify(IdTokens.sign(key))
        server.failing = true
        Thread.sleep(400)

        // when
        val identity = verifier.verify(IdTokens.sign(key, sub = "during-outage"))

        // then
        assertThat(identity.providerUserId).isEqualTo("during-outage")
    }
}
