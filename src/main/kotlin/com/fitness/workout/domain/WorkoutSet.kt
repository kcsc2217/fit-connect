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
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * 세트 기록. user_id와 performed_at은 정규화상 불필요하지만 의도적으로 둔다:
 * 멱등성 유니크 범위를 사용자 단위로 잡고, "이 종목의 최근 2주 기록"을 세션 조인 없이 인덱스 하나로 조회하기 위해서다.
 */
@Entity
@Table(
    name = "workout_set",
    uniqueConstraints = [UniqueConstraint(name = "uk_workout_set_user_client", columnNames = ["user_id", "client_id"])],
    indexes = [
        Index(name = "idx_workout_set_session", columnList = "session_id"),
        Index(name = "idx_workout_set_user_exercise_performed", columnList = "user_id, exercise_id, performed_at"),
    ],
)
class WorkoutSet(

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val clientId: UUID,

    @Column(nullable = false)
    val sessionId: Long,

    @Column(nullable = false)
    val exerciseId: Long,

    // 세션 안 종목 순서
    @Column(nullable = false)
    var exerciseOrder: Short,

    // 종목 안 세트 순서
    @Column(nullable = false)
    var setOrder: Short,

    // 맨몸은 0
    @Column(nullable = false, precision = 6, scale = 2)
    var weightKg: BigDecimal,

    @Column(nullable = false)
    var reps: Short,

    // 체감 강도
    @Column(precision = 3, scale = 1)
    var rpe: BigDecimal? = null,

    @Column(nullable = false)
    var completed: Boolean,

    @Column(nullable = false)
    var deleted: Boolean = false,

    @Column(nullable = false)
    var performedAt: Instant,

    @Column(nullable = false)
    var clientUpdatedAt: Instant,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}
