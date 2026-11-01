package com.fitness.support

import com.fitness.common.type.Experience
import com.fitness.workout.domain.BodyPart
import com.fitness.workout.domain.Exercise
import com.fitness.workout.domain.LoadType
import com.fitness.workout.domain.WorkoutSession
import com.fitness.workout.domain.WorkoutSet
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

object WorkoutFixtures {

    private val now: Instant = Instant.parse("2026-11-01T09:00:00Z")

    fun exercise(
        code: String,
        bodyPart: BodyPart = BodyPart.CHEST,
        active: Boolean = true,
    ) = Exercise(
        code = code,
        nameKo = "테스트 종목",
        nameEn = "Test Exercise",
        bodyPart = bodyPart,
        loadType = LoadType.BARBELL,
        minLevel = Experience.BEGINNER,
        compound = true,
        defaultRestSec = 120,
        active = active,
    )

    fun session(userId: Long, clientId: UUID = UUID.randomUUID()) = WorkoutSession(
        userId = userId,
        clientId = clientId,
        startedAt = now,
        clientUpdatedAt = now,
        serverReceivedAt = now,
    )

    fun set(
        userId: Long,
        sessionId: Long,
        exerciseId: Long,
        clientId: UUID = UUID.randomUUID(),
        weightKg: BigDecimal = BigDecimal("60.00"),
    ) = WorkoutSet(
        userId = userId,
        clientId = clientId,
        sessionId = sessionId,
        exerciseId = exerciseId,
        exerciseOrder = 1,
        setOrder = 1,
        weightKg = weightKg,
        reps = 10,
        completed = true,
        performedAt = now,
        clientUpdatedAt = now,
    )
}
