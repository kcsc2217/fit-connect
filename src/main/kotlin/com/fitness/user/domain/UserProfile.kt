package com.fitness.user.domain

import com.fitness.common.entity.BaseTimeEntity
import com.fitness.common.type.Experience
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalTime

/** 온보딩 프로필. users와 1:1이고 PK가 곧 user_id다. */
@Entity
@Table(
    name = "user_profile",
    // 1분 스케줄러가 "알림 시각이 30분 안에 다가온 사용자"를 조회
    indexes = [Index(name = "idx_user_profile_alarm_time", columnList = "alarm_time")],
)
class UserProfile(

    @Id
    val userId: Long,

    @Column(nullable = false, precision = 5, scale = 1)
    var heightCm: BigDecimal,

    @Column(nullable = false, precision = 5, scale = 1)
    var weightKg: BigDecimal,

    // 나이는 매년 바뀌므로 출생 연도를 저장
    @Column(nullable = false)
    var birthYear: Short,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var sex: Sex,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var goal: Goal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var experience: Experience,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var splitType: SplitType,

    workoutDays: Int,

    // 사용자 시간대 기준 알림 시각
    @Column(nullable = false)
    var alarmTime: LocalTime,

    @Column(nullable = false, length = 40)
    var timezone: String = "Asia/Seoul",

    // 선호 운동 시간 (루틴 시간 검증용)
    @Column(nullable = false)
    var sessionMinutes: Short,

) : BaseTimeEntity() {

    // 요일 비트마스크 (월=1, 화=2, 수=4, 목=8, 금=16, 토=32, 일=64)
    // DDL이 TINYINT라 범위를 넘으면 오류 없이 다른 값(128 → -128)으로 저장되므로 여기서 막는다.
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(nullable = false)
    var workoutDays: Int = validWorkoutDays(workoutDays)
        set(value) {
            field = validWorkoutDays(value)
        }

    companion object {
        val WORKOUT_DAYS_RANGE = 0..127

        private fun validWorkoutDays(value: Int): Int {
            require(value in WORKOUT_DAYS_RANGE) { "운동 요일 비트마스크는 0~127이어야 합니다: $value" }
            return value
        }
    }
}
