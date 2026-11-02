package com.fitness.support

import com.fitness.llm.domain.LlmCallLog
import com.fitness.llm.domain.LlmFeature
import com.fitness.llm.domain.ValidationResult
import com.fitness.notification.domain.PushLog
import com.fitness.notification.domain.PushType
import com.fitness.routine.domain.DailyRoutine
import com.fitness.routine.domain.RoutineItem
import com.fitness.routine.domain.RoutineSource
import com.fitness.routine.domain.RoutineStatus
import com.fitness.routine.domain.SplitDay
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** 오늘의 운동 흐름(루틴 → 알림, LLM 호출 로그) 픽스처 */
object RoutineFixtures {

    private val today: LocalDate = LocalDate.of(2026, 11, 2)
    private val now: Instant = Instant.parse("2026-11-01T22:00:00Z")

    fun routine(userId: Long, routineDate: LocalDate = today) = DailyRoutine(
        userId = userId,
        routineDate = routineDate,
        status = RoutineStatus.GENERATED,
        source = RoutineSource.LLM,
        splitDay = SplitDay.LOWER,
    )

    fun item(routineId: Long, exerciseId: Long = 1L) = RoutineItem(
        routineId = routineId,
        exerciseId = exerciseId,
        itemOrder = 1,
        sets = 3,
        reps = 10,
        targetWeightKg = BigDecimal("60.00"),
        restSec = 120,
    )

    fun push(
        userId: Long,
        refId: Long,
        type: PushType = PushType.DAILY_ROUTINE,
        scheduledAt: Instant = now,
    ) = PushLog(
        userId = userId,
        type = type,
        refId = refId,
        scheduledAt = scheduledAt,
    )

    fun llmCall(
        userId: Long? = 1L,
        refId: Long? = null,
        failureReason: String? = null,
    ) = LlmCallLog(
        feature = LlmFeature.ROUTINE,
        userId = userId,
        refId = refId,
        promptVersion = "routine-v1",
        model = "test-model",
        inputTokens = 1200,
        outputTokens = 400,
        latencyMs = 2500,
        retryCount = 0,
        validationResult = ValidationResult.PASS,
        failureReason = failureReason,
        fallbackUsed = false,
    )
}
