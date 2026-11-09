package com.fitness.user.client

import com.fitness.user.domain.AuthProvider
import java.time.Duration

/** provider별 id_token 검증 설정. 공개키 캐시·제한 값의 이유는 ADR 0005 */
data class OidcProviderSettings(
    val provider: AuthProvider,
    val issuers: Set<String>,
    val audiences: Set<String>,
    val jwkSetUri: String,
    val connectTimeout: Duration = Duration.ofSeconds(1),
    val readTimeout: Duration = Duration.ofSeconds(1),
    val cacheTtl: Duration = Duration.ofHours(1),
    val refetchMinInterval: Duration = Duration.ofSeconds(30),
    val outageTolerance: Duration = Duration.ofHours(12),
) {
    companion object {
        // 값은 각 회사의 OpenID 설정 문서(.well-known/openid-configuration)로 확인 (2026-11-06)
        private const val KAKAO_ISSUER = "https://kauth.kakao.com"
        private const val KAKAO_JWK_SET_URI = "https://kauth.kakao.com/.well-known/jwks.json"
        private val GOOGLE_ISSUERS = setOf("https://accounts.google.com", "accounts.google.com")
        private const val GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs"

        fun kakao(p: SocialLoginProperties.Provider) = OidcProviderSettings(
            provider = AuthProvider.KAKAO,
            issuers = setOf(KAKAO_ISSUER),
            audiences = p.audiences.filter { it.isNotBlank() }.toSet(),
            jwkSetUri = p.jwkSetUri ?: KAKAO_JWK_SET_URI,
        )

        fun google(p: SocialLoginProperties.Provider) = OidcProviderSettings(
            provider = AuthProvider.GOOGLE,
            issuers = GOOGLE_ISSUERS,
            audiences = p.audiences.filter { it.isNotBlank() }.toSet(),
            jwkSetUri = p.jwkSetUri ?: GOOGLE_JWK_SET_URI,
        )
    }
}
