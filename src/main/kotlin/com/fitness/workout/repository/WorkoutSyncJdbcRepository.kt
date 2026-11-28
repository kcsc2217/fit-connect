package com.fitness.workout.repository

import com.fitness.workout.dto.SessionChange
import com.fitness.workout.dto.SetChange
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * 동기화용 SQL (계획 011). JPA save는 중복 INSERT 예외가 나면 트랜잭션이 롤백 전용이 되어 묶음 전체가 실패하므로,
 * INSERT 실패(중복 키)를 잡고 같은 트랜잭션에서 조건부 UPDATE로 넘어갈 수 있게 SQL을 직접 쓴다.
 */
@Repository
class WorkoutSyncJdbcRepository(private val jdbc: NamedParameterJdbcTemplate) {

    /** 이미 서버에 있는 세션 client_id */
    fun findSessionClientIds(userId: Long, clientIds: Collection<UUID>): Set<UUID> = TODO("011 구현")

    /** 세션 client_id → 서버 id (같은 사용자 것만) */
    fun findSessionIds(userId: Long, clientIds: Collection<UUID>): Map<UUID, Long> = TODO("011 구현")

    /** 이미 서버에 있는 세트 client_id */
    fun findSetClientIds(userId: Long, clientIds: Collection<UUID>): Set<UUID> = TODO("011 구현")

    /** 중복 키면 DuplicateKeyException */
    fun insertSession(userId: Long, s: SessionChange, clientUpdatedAt: Instant, now: Instant): Unit = TODO("011 구현")

    /** 삭제되지 않았고 더 새로운 수정일 때만 덮어쓴다. 바뀐 행 수(0 또는 1) */
    fun updateSession(userId: Long, s: SessionChange, clientUpdatedAt: Instant, now: Instant): Int = TODO("011 구현")

    /** 중복 키면 DuplicateKeyException */
    fun insertSet(userId: Long, sessionId: Long, t: SetChange, clientUpdatedAt: Instant, now: Instant): Unit =
        TODO("011 구현")

    /** 삭제되지 않았고 더 새로운 수정일 때만 덮어쓴다. session_id·exercise_id는 바꾸지 않는다. 바뀐 행 수(0 또는 1) */
    fun updateSet(userId: Long, t: SetChange, clientUpdatedAt: Instant, now: Instant): Int = TODO("011 구현")
}
