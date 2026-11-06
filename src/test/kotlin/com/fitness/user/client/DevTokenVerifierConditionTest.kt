package com.fitness.user.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.core.io.ClassPathResource

/** 개발용 로그인은 설정값으로만 켜진다 (계획 006 1-6). 운영 설정에는 이 값이 없다 */
class DevTokenVerifierConditionTest {

    private val runner = ApplicationContextRunner().withUserConfiguration(DevTokenVerifier::class.java)

    @Test
    fun `개발 로그인 설정이 없으면 DevTokenVerifier 빈이 생기지 않는다`() {
        // given
        val withoutSetting = runner

        // when & then
        withoutSetting.run { context -> assertThat(context).doesNotHaveBean(DevTokenVerifier::class.java) }
    }

    @Test
    fun `운영 기준 설정(application yml)만으로 띄우면 DevTokenVerifier 빈이 생기지 않는다`() {
        // given: 운영 jar에 들어가는 application.yml만 읽는다 (local·test 설정은 섞지 않는다)
        val productionConfig = YamlPropertySourceLoader().load("application.yml", ClassPathResource("application.yml"))
        val withProductionConfig = runner.withInitializer { context ->
            productionConfig.forEach { context.environment.propertySources.addLast(it) }
        }

        // when & then
        withProductionConfig.run { context -> assertThat(context).doesNotHaveBean(DevTokenVerifier::class.java) }
    }

    @Test
    fun `개발 로그인 설정을 켜면 DevTokenVerifier 빈이 생긴다`() {
        // given
        val withSetting = runner.withPropertyValues("fitness.auth.dev-login.enabled=true")

        // when & then
        withSetting.run { context -> assertThat(context).hasSingleBean(DevTokenVerifier::class.java) }
    }
}
