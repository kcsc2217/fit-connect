package com.fitness.user.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.WEDNESDAY

class WorkoutDaysTest {

    @Test
    fun `월·수·금은 비트마스크 21이 되고 21은 다시 월·수·금이 된다`() {
        // given
        val days = listOf(FRIDAY, MONDAY, WEDNESDAY)

        // when
        val mask = WorkoutDays.toMask(days)

        // then
        assertThat(mask to WorkoutDays.fromMask(mask)).isEqualTo(21 to listOf(MONDAY, WEDNESDAY, FRIDAY))
    }

    @Test
    fun `일요일만 고르면 비트마스크 64가 된다`() {
        // given
        val days = listOf(SUNDAY)

        // when
        val mask = WorkoutDays.toMask(days)

        // then
        assertThat(mask).isEqualTo(64)
    }
}
