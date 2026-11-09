package com.fitness.user.client

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.jwk.source.URLBasedJWKSetSource
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jose.util.DefaultResourceRetriever
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.net.URI
import java.time.Duration

/**
 * 카카오·구글 id_token decoder 조립 (ADR 0005).
 * Spring 기본 빌더(withJwkSetUri)는 모르는 kid마다 공개키를 제한 없이 다시 받으므로, 공개키 소스를 직접 조립한다.
 */
object OidcJwtDecoders {

    private const val JWK_SET_SIZE_LIMIT = 50 * 1024 // 50KB
    private const val CLOCK_SKEW_SECONDS = 30L
    private const val MAX_SUBJECT_LENGTH = 100 // social_account.provider_user_id VARCHAR(100)

    fun create(settings: OidcProviderSettings): OidcJwtDecoder {
        val retriever = DefaultResourceRetriever(
            settings.connectTimeout.toMillis().toInt(),
            settings.readTimeout.toMillis().toInt(),
            JWK_SET_SIZE_LIMIT,
        )
        // 실제 HTTP 받기를 가장 안쪽에 두고 결과를 기록한다 (401·503 구분용). 그 위에 캐시·제한·재시도·장애 허용을 얹는다
        val keySource = FailureTrackingJWKSetSource(URLBasedJWKSetSource(URI(settings.jwkSetUri).toURL(), retriever))
        // 모르는 kid가 와도 다시 받기는 rateLimited 간격에 두 번까지(처음 받기 포함, 실패한 받기도 센다). 장애 때는 받아 둔 키를 outageTolerant 동안 쓴다
        val jwkSource = JWKSourceBuilder.create(keySource)
            .cache(settings.cacheTtl.toMillis(), settings.readTimeout.toMillis() * 2)
            .refreshAheadCache(false) // 백그라운드 스레드를 만들지 않는다
            .rateLimited(settings.refetchMinInterval.toMillis())
            .retrying(true)
            .outageTolerant(settings.outageTolerance.toMillis())
            .build()

        val processor = DefaultJWTProcessor<SecurityContext>().apply {
            jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource) // RS256만 받는다
            jwtClaimsSetVerifier = null // 클레임 검사는 아래 Spring 검증기 한곳에서만 한다
        }
        val decoder = NimbusJwtDecoder(processor).apply { setJwtValidator(validator(settings)) }
        return OidcJwtDecoder(decoder, keySource)
    }

    private fun validator(settings: OidcProviderSettings): OAuth2TokenValidator<Jwt> =
        DelegatingOAuth2TokenValidator(
            JwtTimestampValidator(Duration.ofSeconds(CLOCK_SKEW_SECONDS)),
            // JwtTimestampValidator는 exp가 없으면 통과시킨다. 만료 없는 토큰을 따로 거절한다
            rule("exp 없음") { it.expiresAt != null },
            // iss는 URL이 아닐 수도 있어(구글 accounts.google.com) 문자열로 비교한다
            rule("허용되지 않은 발급자") { it.getClaimAsString(JwtClaimNames.ISS) in settings.issuers },
            rule("허용되지 않은 앱") { jwt -> jwt.audience.orEmpty().any { it in settings.audiences } },
            rule("sub 형식 오류") {
                val sub = it.subject
                !sub.isNullOrBlank() && sub.length <= MAX_SUBJECT_LENGTH
            },
        )

    private fun rule(description: String, check: (Jwt) -> Boolean) = OAuth2TokenValidator<Jwt> { jwt ->
        if (check(jwt)) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token", description, null))
        }
    }
}
