package com.fitness.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.time.Instant
import java.util.Optional

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
class JpaAuditingConfig {

    // 감사 시각을 서버 시간대와 무관한 Instant로 채운다
    @Bean
    fun utcDateTimeProvider() = DateTimeProvider { Optional.of(Instant.now()) }
}
