package com.fitness.workout.repository

import com.fitness.workout.domain.WorkoutSession
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface WorkoutSessionRepository : JpaRepository<WorkoutSession, Long> {
    fun findByUserIdAndClientId(userId: Long, clientId: UUID): WorkoutSession?
}
