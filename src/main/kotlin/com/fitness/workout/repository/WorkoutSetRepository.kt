package com.fitness.workout.repository

import com.fitness.workout.domain.WorkoutSet
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface WorkoutSetRepository : JpaRepository<WorkoutSet, Long> {
    fun findByUserIdAndClientId(userId: Long, clientId: UUID): WorkoutSet?
}
