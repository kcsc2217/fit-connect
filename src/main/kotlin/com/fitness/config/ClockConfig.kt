package com.fitness.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/** 현재 시각은 주입받은 Clock으로 읽는다. 테스트에서 시각을 고정하기 위해서다 (토큰 만료 등) */
@Configuration
class ClockConfig {

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
