package com.fitness.user.client

import com.fitness.user.domain.AuthProvider
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.core.type.AnnotatedTypeMetadata

/**
 * 카카오·구글 id_token 확인기 (ADR 0005). 허용 aud가 설정된 provider만 등록한다.
 * 등록하지 않은 provider는 SocialTokenVerifiers가 400 UNSUPPORTED_PROVIDER로 응답해 설정 누락이 드러난다.
 */
@Configuration
class SocialLoginConfig(private val properties: SocialLoginProperties) {

    @Bean
    @Conditional(OnKakaoConfigured::class)
    fun kakaoIdTokenVerifier(): SocialTokenVerifier =
        OidcIdTokenVerifier(AuthProvider.KAKAO, OidcJwtDecoders.create(OidcProviderSettings.kakao(properties.kakao)))

    @Bean
    @Conditional(OnGoogleConfigured::class)
    fun googleIdTokenVerifier(): SocialTokenVerifier =
        OidcIdTokenVerifier(AuthProvider.GOOGLE, OidcJwtDecoders.create(OidcProviderSettings.google(properties.google)))
}

/**
 * 허용 aud가 하나라도 있으면 true. 목록은 YAML 목록으로도 쉼표 문자열로도 들어올 수 있어 실제 바인딩 결과를 본다.
 * 환경변수가 비면("") 빈 문자열 하나로 바인딩될 수 있어 공백은 빼고 센다.
 */
abstract class OnAudiencesConfigured(private val prefix: String) : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        Binder.get(context.environment)
            .bind("$prefix.audiences", Bindable.listOf(String::class.java))
            .map { audiences -> audiences.any { it.isNotBlank() } }
            .orElse(false)
}

class OnKakaoConfigured : OnAudiencesConfigured("fitness.auth.kakao")

class OnGoogleConfigured : OnAudiencesConfigured("fitness.auth.google")
