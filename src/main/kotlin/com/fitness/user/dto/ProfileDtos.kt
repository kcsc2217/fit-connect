package com.fitness.user.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fitness.common.type.Equipment
import com.fitness.common.type.Experience
import com.fitness.user.domain.Goal
import com.fitness.user.domain.Sex
import com.fitness.user.domain.SplitType
import com.fitness.user.domain.UserProfile
import com.fitness.user.domain.UserStatus
import com.fitness.user.domain.WorkoutDays
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * 온보딩 제출 = 프로필·장비 덮어쓰기 (계획 009).
 * 고정 범위만 여기서 검증하고, 올해·시간대가 필요한 값(출생 연도 등)은 UserProfileService가 검증한다.
 */
data class ProfileRequest(
    // 소수 둘째 자리는 DB(DECIMAL(5,1))가 조용히 반올림하므로 막는다
    @field:DecimalMin("100.0")
    @field:DecimalMax("250.0")
    @field:Digits(integer = 3, fraction = 1)
    val heightCm: BigDecimal,

    @field:DecimalMin("30.0")
    @field:DecimalMax("300.0")
    @field:Digits(integer = 3, fraction = 1)
    val weightKg: BigDecimal,

    val birthYear: Short,
    val sex: Sex,
    val goal: Goal,
    val experience: Experience,

    // 빈 목록 허용: 맨몸 종목만 후보가 된다.
    // 원소가 nullable인 이유: Jackson은 ["BARBELL", null]의 null을 그대로 넣는다. 원소 @NotNull은 Kotlin이
    // 타입 어노테이션을 바이트코드에 쓰지 않아 동작하지 않으므로, null은 UserProfileService가 400으로 막는다
    val equipment: List<Equipment?>,

    @field:NotEmpty
    val workoutDays: List<DayOfWeek?>,

    val splitType: SplitType,

    @field:JsonFormat(pattern = "HH:mm")
    val alarmTime: LocalTime,

    val timezone: String,
    val sessionMinutes: Short,
)

data class ProfileResponse(
    val heightCm: BigDecimal,
    val weightKg: BigDecimal,
    val birthYear: Short,
    val sex: Sex,
    val goal: Goal,
    val experience: Experience,
    val equipment: List<Equipment>,
    val workoutDays: List<DayOfWeek>,
    val splitType: SplitType,
    @field:JsonFormat(pattern = "HH:mm")
    val alarmTime: LocalTime,
    val timezone: String,
    val sessionMinutes: Short,
    val status: UserStatus,
) {
    companion object {
        /** 장비는 enum 선언 순서, 요일은 월요일부터 */
        fun of(profile: UserProfile, equipment: Collection<Equipment>, status: UserStatus) = ProfileResponse(
            heightCm = profile.heightCm,
            weightKg = profile.weightKg,
            birthYear = profile.birthYear,
            sex = profile.sex,
            goal = profile.goal,
            experience = profile.experience,
            equipment = equipment.distinct().sorted(),
            workoutDays = WorkoutDays.fromMask(profile.workoutDays),
            splitType = profile.splitType,
            alarmTime = profile.alarmTime,
            timezone = profile.timezone,
            sessionMinutes = profile.sessionMinutes,
            status = status,
        )
    }
}
