package com.fitness.workout.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * 운동 기록 묶음 동기화 (계획 011). 세션·세트 합계 1~100건.
 * 항목 값은 Bean Validation을 걸지 않는다: 값이 틀린 항목만 FAILED로 돌려주고 나머지는 저장해야 해서
 * SyncItemValidator가 항목마다 검증한다. 형식 오류(UUID·시각 해석 실패)는 요청 전체를 400으로 막는다.
 */
data class SyncRequest(
    val sessions: List<SessionChange> = emptyList(),
    val sets: List<SetChange> = emptyList(),
)

data class SessionChange(
    val clientId: UUID,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val conditionNote: String? = null,
    val deleted: Boolean = false,
    val clientUpdatedAt: Instant,
)

data class SetChange(
    val clientId: UUID,
    // 세트는 서버 id가 아니라 앱이 만든 세션 client_id로 세션을 가리킨다 (오프라인에서는 서버 id가 없음)
    val sessionClientId: UUID,
    val exerciseId: Long,
    val exerciseOrder: Int,
    val setOrder: Int,
    val weightKg: BigDecimal,
    val reps: Int,
    val rpe: BigDecimal? = null,
    val completed: Boolean,
    val deleted: Boolean = false,
    val performedAt: Instant,
    val clientUpdatedAt: Instant,
)

data class SyncResponse(val results: List<ItemResult>)

data class ItemResult(
    val clientId: UUID,
    val type: ItemType,
    val outcome: Outcome,
    val reason: FailReason? = null,
)

enum class ItemType { SESSION, SET }

enum class Outcome {
    /** 새로 넣었거나 더 새로운 값으로 덮어씀 */
    APPLIED,

    /** 더 새로운 값이 이미 있거나 삭제된 행이라 바꾸지 않음 (성공으로 본다) */
    IGNORED,

    /** 저장하지 않음 (reason 참고) */
    FAILED,
}

enum class FailReason {
    INVALID_VALUE,
    UNKNOWN_EXERCISE,
    SESSION_NOT_FOUND,
}
