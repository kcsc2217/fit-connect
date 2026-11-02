package com.fitness.challenge.domain

import com.fitness.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import java.time.Instant

/** 챌린지 신청. 사용자당 챌린지마다 한 행이고, 취소·재신청·대기·만료는 상태로 표현한다. */
@Entity
@Table(
    name = "challenge_participation",
    // 최후의 중복 신청 안전망. 어떤 동시성 방식을 쓰든 같은 사용자의 두 번째 행은 DB가 막는다
    uniqueConstraints = [
        UniqueConstraint(name = "uk_challenge_participation_challenge_user", columnNames = ["challenge_id", "user_id"]),
    ],
    indexes = [
        // 대기 1순위 조회
        Index(name = "idx_challenge_participation_waiting", columnList = "challenge_id, status, wait_seq"),
        // 임시 확보 만료 처리
        Index(name = "idx_challenge_participation_hold_expires", columnList = "status, hold_expires_at"),
    ],
)
class ChallengeParticipation(

    // 같은 모듈이라 FK 있음 (DDL)
    @Column(nullable = false)
    val challengeId: Long,

    // 모듈 간 참조라 FK 없음
    @Column(nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ParticipationStatus,

    // 대기 순번
    var waitSeq: Int? = null,

    // 임시 확보 만료 시각 (10분, 승급자는 30분)
    var holdExpiresAt: Instant? = null,

    // 확정 시각
    var appliedAt: Instant? = null,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Version
    @Column(nullable = false)
    var version: Int = 0
        protected set
}
