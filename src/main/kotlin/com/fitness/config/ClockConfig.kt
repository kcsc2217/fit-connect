package com.fitness.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * 현재 시각은 Instant.now()가 아니라 이 Clock으로 읽는다. 테스트에서 시각을 고정할 수 있고(토큰 만료 등),
 * DB 정밀도와 같은 마이크로초 단위로 끊는다.
 */
@Configuration
class ClockConfig {

    @Bean
    fun clock(): Clock = microsecondClock(Clock.systemUTC())

    companion object {
        /**
         * DATETIME(6)과 같은 마이크로초 단위로 끊는다.
         * 리눅스(CI·운영) 시계는 나노초까지 있어 MySQL이 6자리로 반올림해 저장하므로, 그대로 쓰면 메모리 값과 DB 값이 달라진다.
         * 맥 시계는 원래 마이크로초라 로컬에서는 드러나지 않는다.
         */
        fun microsecondClock(base: Clock): Clock = Clock.tick(base, Duration.of(1, ChronoUnit.MICROS))
    }
}
