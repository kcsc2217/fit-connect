package com.fitness.user.client

import org.springframework.boot.context.properties.ConfigurationProperties

/** 소셜 로그인 설정 (ADR 0005). 허용 aud가 비면 그 provider 확인기를 만들지 않는다 */
@ConfigurationProperties("fitness.auth")
data class SocialLoginProperties(
    val kakao: Provider = Provider(),
    val google: Provider = Provider(),
) {
    data class Provider(
        val audiences: List<String> = emptyList(),
        val jwkSetUri: String? = null, // 테스트에서 가짜 공개키 서버로 바꿀 때만 쓴다
    )
}
