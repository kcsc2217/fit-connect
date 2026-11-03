package com.fitness.support

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * API 통합 테스트 공통 설정.
 * 앱 전체를 띄우고 MockMvc로 요청한다. DB는 @RepositoryTest와 같은 fitness_test + Flyway.
 * Redisson이 시작할 때 Redis에 연결하므로 로컬 Redis도 켜져 있어야 한다.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local", "test")
@Import(TestFlywayConfig::class)
annotation class IntegrationTest
