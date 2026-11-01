package com.fitness.support

import com.fitness.user.domain.Experience
import com.fitness.user.domain.Goal
import com.fitness.user.domain.Sex
import com.fitness.user.domain.SplitType
import com.fitness.user.domain.User
import com.fitness.user.domain.UserProfile
import java.math.BigDecimal
import java.time.LocalTime

object UserFixtures {

    fun user(nickname: String = "테스터") = User(nickname = nickname)

    fun profile(
        userId: Long,
        workoutDays: Int = 1 or 4 or 16, // 월, 수, 금
    ) = UserProfile(
        userId = userId,
        heightCm = BigDecimal("175.0"),
        weightKg = BigDecimal("72.5"),
        birthYear = 1995,
        sex = Sex.MALE,
        goal = Goal.BULK,
        experience = Experience.INTERMEDIATE,
        splitType = SplitType.THREE_SPLIT,
        workoutDays = workoutDays,
        alarmTime = LocalTime.of(18, 30),
        sessionMinutes = 60,
    )
}
