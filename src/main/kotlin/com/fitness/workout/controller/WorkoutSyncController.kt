package com.fitness.workout.controller

import com.fitness.workout.dto.SyncRequest
import com.fitness.workout.dto.SyncResponse
import com.fitness.workout.service.WorkoutSyncService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "운동 기록")
@RestController
@RequestMapping("/api/v1/workouts")
class WorkoutSyncController(private val workoutSyncService: WorkoutSyncService) {

    @Operation(
        summary = "운동 기록 묶음 동기화",
        description = "세션·세트 변경을 합계 1~100건 보낸다. 같은 항목을 다시 보내도 한 번만 저장되고(멱등), " +
            "수정 시각이 더 새로운 쪽이 이긴다. 결과는 항목별(APPLIED·IGNORED·FAILED)로 요청 순서대로 돌려준다.",
        security = [SecurityRequirement(name = "bearer")],
    )
    @PostMapping("/sync")
    fun sync(
        @Parameter(hidden = true) @AuthenticationPrincipal userId: Long,
        @RequestBody request: SyncRequest,
    ): SyncResponse = workoutSyncService.sync(userId, request)
}
