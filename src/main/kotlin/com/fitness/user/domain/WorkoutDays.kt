package com.fitness.user.domain

import java.time.DayOfWeek

/**
 * 운동 요일 목록 ↔ user_profile.workout_days 비트마스크 (월=1, 화=2, 수=4 … 일=64, db-schema).
 * API는 요일 목록으로 주고받고, 비트마스크는 저장 형식이라 변환은 여기서만 한다 (계획 009).
 */
object WorkoutDays {

    fun toMask(days: Collection<DayOfWeek>): Int = days.fold(0) { acc, day -> acc or bitOf(day) }

    /** 월요일부터 순서대로 */
    fun fromMask(mask: Int): List<DayOfWeek> = DayOfWeek.entries.filter { mask and bitOf(it) != 0 }

    // DayOfWeek.value는 월=1 … 일=7
    private fun bitOf(day: DayOfWeek): Int = 1 shl (day.value - 1)
}
