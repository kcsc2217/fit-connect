package com.fitness.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.time.Clock
import java.util.Optional

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
class JpaAuditingConfig {

    // 감사 시각을 서버 시간대와 무관한 Instant로 채운다. 앱 Clock(마이크로초, ClockConfig)을 써서 저장 직후 메모리 값과 DB 값이 같다
    @Bean
    fun utcDateTimeProvider(clock: Clock) = DateTimeProvider { Optional.of(clock.instant()) }
}
