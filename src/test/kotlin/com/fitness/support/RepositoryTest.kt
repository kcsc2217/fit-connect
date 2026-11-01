package com.fitness.support

import com.fitness.config.JpaAuditingConfig
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * 저장소 테스트 공통 설정.
 * 로컬 MySQL의 fitness_test DB에 Flyway 마이그레이션을 적용하고, 엔티티와 스키마를 validate로 비교한다.
 * 테스트마다 트랜잭션이 롤백된다.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local", "test")
@Import(JpaAuditingConfig::class, TestFlywayConfig::class)
annotation class RepositoryTest
