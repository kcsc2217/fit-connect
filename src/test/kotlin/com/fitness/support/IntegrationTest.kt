package com.fitness.support

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

/**
 * API 통합 테스트 공통 설정.
 * 앱 전체를 띄우고 MockMvc로 요청한다. DB는 @RepositoryTest와 같은 fitness_test + Flyway.
 * Redisson이 시작할 때 Redis에 연결하므로 로컬 Redis도 켜져 있어야 한다.
 * 카카오·구글 공개키는 테스트 JVM 안의 가짜 서버(FakeOidcProviders)에서 받는다.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local", "test")
@Import(TestFlywayConfig::class)
@ContextConfiguration(initializers = [FakeOidcInitializer::class])
annotation class IntegrationTest
