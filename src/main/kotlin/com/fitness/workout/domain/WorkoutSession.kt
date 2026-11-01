package com.fitness.workout.domain

import com.fitness.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

/** 운동 세션. 앱이 로컬에 먼저 저장하고, 연결되면 묶음으로 동기화한다. */
@Entity
@Table(
    name = "workout_session",
    // 멱등성 핵심: 재전송돼도 한 번만 저장. 범위는 사용자 단위
    uniqueConstraints = [UniqueConstraint(name = "uk_workout_session_user_client", columnNames = ["user_id", "client_id"])],
    indexes = [Index(name = "idx_workout_session_user_started", columnList = "user_id, started_at")],
)
class WorkoutSession(

    // 모듈 간 참조라 FK 없음
    @Column(nullable = false)
    val userId: Long,

    // 앱이 만든 UUID (BINARY(16))
    @Column(nullable = false)
    val clientId: UUID,

    // 어떤 루틴으로 한 운동인지 (routine 모듈, FK 없음)
    var routineId: Long? = null,

    @Column(nullable = false)
    var startedAt: Instant,

    var endedAt: Instant? = null,

    @Column(length = 200)
    var conditionNote: String? = null,

    // tombstone: 늦게 도착한 수정이 삭제된 기록을 되살리지 않게
    @Column(nullable = false)
    var deleted: Boolean = false,

    // 충돌 판정 기준 (last-write-wins)
    @Column(nullable = false)
    var clientUpdatedAt: Instant,

    // 기기 시계 어긋남 확인용
    @Column(nullable = false)
    var serverReceivedAt: Instant,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}
