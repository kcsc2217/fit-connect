package com.fitness.workout.service

import com.fitness.user.service.UserService
import com.fitness.workout.dto.SyncRequest
import com.fitness.workout.dto.SyncResponse
import com.fitness.workout.repository.WorkoutSyncJdbcRepository
import org.springframework.stereotype.Service
import java.time.Clock

/** 운동 기록 묶음 동기화 (계획 011) */
@Service
class WorkoutSyncService(
    private val repo: WorkoutSyncJdbcRepository,
    private val validator: SyncItemValidator,
    private val exerciseService: ExerciseService,
    private val userService: UserService,
    private val clock: Clock,
) {
    /** 항목마다 upsert(last-write-wins, tombstone)하고 요청 순서대로 결과를 돌려준다 */
    fun sync(userId: Long, request: SyncRequest): SyncResponse = TODO("011 구현")
}
