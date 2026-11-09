package com.fitness.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ClockConfigTest {

    @Test
    fun `앱 시계는 나노초가 있는 시각도 마이크로초 단위로 끊는다`() {
        // given: 리눅스 시계처럼 나노초까지 있는 시각 (MySQL DATETIME(6)은 이 값을 반올림해 저장한다)
        val linuxLike = Clock.fixed(Instant.parse("2026-11-09T00:00:00.123456789Z"), ZoneOffset.UTC)

        // when
        val now = ClockConfig.microsecondClock(linuxLike).instant()

        // then: 버림으로 6자리. DB에 저장되는 값과 메모리 값이 같아진다
        assertThat(now).isEqualTo(Instant.parse("2026-11-09T00:00:00.123456Z"))
    }
}
