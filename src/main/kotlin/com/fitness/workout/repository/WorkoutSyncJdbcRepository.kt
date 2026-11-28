package com.fitness.workout.repository

import com.fitness.workout.dto.SessionChange
import com.fitness.workout.dto.SetChange
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * 동기화용 SQL (계획 011). JPA save는 중복 INSERT 예외가 나면 트랜잭션이 롤백 전용이 되어 묶음 전체가 실패하므로,
 * INSERT 실패(중복 키)를 잡고 같은 트랜잭션에서 조건부 UPDATE로 넘어갈 수 있게 SQL을 직접 쓴다.
 * created_at·updated_at은 JPA Auditing을 거치지 않으므로 호출하는 쪽이 준 시각(Clock)을 넣는다.
 */
@Repository
class WorkoutSyncJdbcRepository(private val jdbc: NamedParameterJdbcTemplate) {

    /** 이미 서버에 있는 세션 client_id */
    fun findSessionClientIds(userId: Long, clientIds: Collection<UUID>): Set<UUID> =
        findSessionIds(userId, clientIds).keys

    /** 세션 client_id → 서버 id (같은 사용자 것만) */
    fun findSessionIds(userId: Long, clientIds: Collection<UUID>): Map<UUID, Long> {
        if (clientIds.isEmpty()) return emptyMap()
        return jdbc.query(
            "SELECT client_id, id FROM workout_session WHERE user_id = :userId AND client_id IN (:clientIds)",
            MapSqlParameterSource("userId", userId).addValue("clientIds", clientIds.map(::toBytes)),
        ) { rs, _ -> toUuid(rs.getBytes("client_id")) to rs.getLong("id") }.toMap()
    }

    /** 이미 서버에 있는 세트 client_id */
    fun findSetClientIds(userId: Long, clientIds: Collection<UUID>): Set<UUID> {
        if (clientIds.isEmpty()) return emptySet()
        return jdbc.query(
            "SELECT client_id FROM workout_set WHERE user_id = :userId AND client_id IN (:clientIds)",
            MapSqlParameterSource("userId", userId).addValue("clientIds", clientIds.map(::toBytes)),
        ) { rs, _ -> toUuid(rs.getBytes("client_id")) }.toSet()
    }

    /** 중복 키면 DuplicateKeyException */
    fun insertSession(userId: Long, s: SessionChange, clientUpdatedAt: Instant, now: Instant) {
        jdbc.update(
            """
            INSERT INTO workout_session (user_id, client_id, routine_id, started_at, ended_at, condition_note, deleted,
                                         client_updated_at, server_received_at, created_at, updated_at)
            VALUES (:userId, :clientId, NULL, :startedAt, :endedAt, :conditionNote, :deleted,
                    :clientUpdatedAt, :now, :now, :now)
            """,
            sessionParams(userId, s, clientUpdatedAt, now),
        )
    }

    /** 삭제되지 않았고 더 새로운 수정일 때만 덮어쓴다. 바뀐 행 수(0 또는 1) */
    fun updateSession(userId: Long, s: SessionChange, clientUpdatedAt: Instant, now: Instant): Int =
        jdbc.update(
            """
            UPDATE workout_session
            SET started_at = :startedAt, ended_at = :endedAt, condition_note = :conditionNote, deleted = :deleted,
                client_updated_at = :clientUpdatedAt, server_received_at = :now, updated_at = :now
            WHERE user_id = :userId AND client_id = :clientId
              AND deleted = FALSE AND client_updated_at < :clientUpdatedAt
            """,
            sessionParams(userId, s, clientUpdatedAt, now),
        )

    /** 중복 키면 DuplicateKeyException */
    fun insertSet(userId: Long, sessionId: Long, t: SetChange, clientUpdatedAt: Instant, now: Instant) {
        jdbc.update(
            """
            INSERT INTO workout_set (user_id, client_id, session_id, exercise_id, exercise_order, set_order, weight_kg,
                                     reps, rpe, completed, deleted, performed_at, client_updated_at, created_at, updated_at)
            VALUES (:userId, :clientId, :sessionId, :exerciseId, :exerciseOrder, :setOrder, :weightKg,
                    :reps, :rpe, :completed, :deleted, :performedAt, :clientUpdatedAt, :now, :now)
            """,
            setParams(userId, t, clientUpdatedAt, now).addValue("sessionId", sessionId),
        )
    }

    /** 삭제되지 않았고 더 새로운 수정일 때만 덮어쓴다. session_id·exercise_id는 바꾸지 않는다. 바뀐 행 수(0 또는 1) */
    fun updateSet(userId: Long, t: SetChange, clientUpdatedAt: Instant, now: Instant): Int =
        jdbc.update(
            """
            UPDATE workout_set
            SET exercise_order = :exerciseOrder, set_order = :setOrder, weight_kg = :weightKg, reps = :reps, rpe = :rpe,
                completed = :completed, deleted = :deleted, performed_at = :performedAt,
                client_updated_at = :clientUpdatedAt, updated_at = :now
            WHERE user_id = :userId AND client_id = :clientId
              AND deleted = FALSE AND client_updated_at < :clientUpdatedAt
            """,
            setParams(userId, t, clientUpdatedAt, now),
        )

    private fun sessionParams(userId: Long, s: SessionChange, clientUpdatedAt: Instant, now: Instant) =
        MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("clientId", toBytes(s.clientId))
            .addValue("startedAt", ts(s.startedAt))
            .addValue("endedAt", s.endedAt?.let(::ts))
            .addValue("conditionNote", s.conditionNote)
            .addValue("deleted", s.deleted)
            .addValue("clientUpdatedAt", ts(clientUpdatedAt))
            .addValue("now", ts(now))

    private fun setParams(userId: Long, t: SetChange, clientUpdatedAt: Instant, now: Instant) =
        MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("clientId", toBytes(t.clientId))
            .addValue("exerciseId", t.exerciseId)
            .addValue("exerciseOrder", t.exerciseOrder)
            .addValue("setOrder", t.setOrder)
            .addValue("weightKg", t.weightKg)
            .addValue("reps", t.reps)
            .addValue("rpe", t.rpe)
            .addValue("completed", t.completed)
            .addValue("deleted", t.deleted)
            .addValue("performedAt", ts(t.performedAt))
            .addValue("clientUpdatedAt", ts(clientUpdatedAt))
            .addValue("now", ts(now))

    companion object {
        // DataSource의 connectionTimeZone=UTC가 JdbcTemplate에도 적용돼 Instant 그대로(UTC) 저장된다 (ADR 0002)
        private fun ts(instant: Instant) = Timestamp.from(instant)

        /** UUID → BINARY(16). Hibernate의 BINARY(16) 매핑과 같은 순서(상위 8바이트, 하위 8바이트) */
        fun toBytes(uuid: UUID): ByteArray =
            ByteBuffer.allocate(16).putLong(uuid.mostSignificantBits).putLong(uuid.leastSignificantBits).array()

        fun toUuid(bytes: ByteArray): UUID = ByteBuffer.wrap(bytes).let { UUID(it.long, it.long) }
    }
}
