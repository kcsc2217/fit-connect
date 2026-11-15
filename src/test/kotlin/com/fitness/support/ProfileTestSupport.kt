package com.fitness.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.fitness.common.type.Equipment
import com.fitness.common.type.Experience
import com.fitness.user.domain.Goal
import com.fitness.user.domain.Sex
import com.fitness.user.domain.SplitType
import com.fitness.user.dto.ProfileRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.put
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalTime

/** 온보딩 프로필 API 테스트 도우미 (계획 009) */
object ProfileTestSupport {

    /** 유효한 제출 본문. 필드 하나만 바꿔 끼워 검증 테스트에 쓴다 */
    fun validProfileJson(vararg overrides: Pair<String, Any?>): Map<String, Any?> = linkedMapOf<String, Any?>(
        "heightCm" to 175.0,
        "weightKg" to 72.5,
        "birthYear" to 1997,
        "sex" to "MALE",
        "goal" to "BULK",
        "experience" to "INTERMEDIATE",
        "equipment" to listOf("BARBELL", "DUMBBELL", "BENCH"),
        "workoutDays" to listOf("MONDAY", "WEDNESDAY", "FRIDAY"),
        "splitType" to "THREE_SPLIT",
        "alarmTime" to "19:00",
        "timezone" to "Asia/Seoul",
        "sessionMinutes" to 60,
    ).apply { putAll(overrides) }

    fun validProfileRequest(equipment: List<Equipment> = listOf(Equipment.BARBELL, Equipment.DUMBBELL)) =
        ProfileRequest(
            heightCm = BigDecimal("175.0"),
            weightKg = BigDecimal("72.5"),
            birthYear = 1997,
            sex = Sex.MALE,
            goal = Goal.BULK,
            experience = Experience.INTERMEDIATE,
            equipment = equipment,
            workoutDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            splitType = SplitType.THREE_SPLIT,
            alarmTime = LocalTime.of(19, 0),
            timezone = "Asia/Seoul",
            sessionMinutes = 60,
        )

    fun MockMvc.putProfile(objectMapper: ObjectMapper, accessToken: String?, body: Map<String, Any?>): ResultActionsDsl =
        put("/api/v1/users/me/profile") {
            accessToken?.let { header(HttpHeaders.AUTHORIZATION, "Bearer $it") }
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }
}
