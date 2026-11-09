package com.fitness.support

import com.nimbusds.jose.jwk.RSAKey
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

/** 통합 테스트용 가짜 카카오·구글 공개키 서버. 테스트 JVM 안에서 한 번만 띄운다 */
object FakeOidcProviders {
    val kakaoKey: RSAKey = IdTokens.rsaKey("kakao-test-kid")
    val googleKey: RSAKey = IdTokens.rsaKey("google-test-kid")
    val kakao = FakeJwksServer().apply { keys = listOf(kakaoKey) }
    val google = FakeJwksServer().apply { keys = listOf(googleKey) }
}

/**
 * 통합 테스트 컨텍스트가 가짜 공개키 서버 주소를 쓰게 한다.
 * @DynamicPropertySource는 테스트 클래스마다 적어야 하고, 클래스마다 설정이 달라지면 컨텍스트가 따로 떠 Flyway clean이 반복된다.
 */
class FakeOidcInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(context: ConfigurableApplicationContext) {
        TestPropertyValues.of(
            "fitness.auth.kakao.jwk-set-uri=${FakeOidcProviders.kakao.uri}",
            "fitness.auth.google.jwk-set-uri=${FakeOidcProviders.google.uri}",
        ).applyTo(context)
    }
}
