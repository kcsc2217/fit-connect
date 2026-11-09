package com.fitness.user.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.core.io.ClassPathResource

/** 허용 aud가 있어야만 소셜 확인기를 등록한다 (계획 007 1-3) */
class SocialLoginConfigTest {

    @EnableConfigurationProperties(SocialLoginProperties::class)
    class PropertiesConfig

    private val runner = ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfig::class.java, SocialLoginConfig::class.java)

    @Test
    fun `허용 aud가 비어 있으면 카카오 확인기를 등록하지 않는다`() {
        // given: 운영 기준 설정(application.yml)만 읽는다. KAKAO_APP_KEYS가 없으면 빈 값이다
        val productionConfig = YamlPropertySourceLoader().load("application.yml", ClassPathResource("application.yml"))
        val withProductionConfig = runner.withInitializer { context ->
            productionConfig.forEach { context.environment.propertySources.addLast(it) }
        }

        // when & then
        withProductionConfig.run { context -> assertThat(context).doesNotHaveBean("kakaoIdTokenVerifier") }
    }

    @Test
    fun `허용 aud를 쉼표 목록으로 설정하면 카카오 확인기를 등록한다`() {
        // given
        val configured = runner.withPropertyValues("fitness.auth.kakao.audiences=app-key-1,app-key-2")

        // when & then
        configured.run { context -> assertThat(context).hasBean("kakaoIdTokenVerifier") }
    }
}
