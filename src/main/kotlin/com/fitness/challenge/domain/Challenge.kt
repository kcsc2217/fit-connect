package com.fitness.challenge.domain

import com.fitness.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.time.LocalDate

/** 정원 제한 챌린지. 선착순 신청 동시성 실험(비관적·낙관적 락, Redis, 대기열)의 대상이다. */
@Entity
@Table(name = "challenge")
class Challenge(

    @Column(nullable = false, length = 100)
    var title: String,

    @Column(nullable = false)
    var startsOn: LocalDate,

    @Column(nullable = false)
    var endsOn: LocalDate,

    @Column(nullable = false)
    var capacity: Int,

    // 대기 정원 (정원의 50%). 계산 규칙은 챌린지 생성 기능에서
    @Column(nullable = false)
    var waitlistCapacity: Int,

    // 일반 신청 오픈 시각
    @Column(nullable = false)
    var openAt: Instant,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ChallengeStatus = ChallengeStatus.SCHEDULED,

    // 현재 신청 수 (DB 락 실험용)
    @Column(nullable = false)
    var appliedCount: Int = 0,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    // 낙관적 락 실험이 그대로 쓰는 버전. UPDATE ... WHERE version = ?가 0건이면 예외
    @Version
    @Column(nullable = false)
    var version: Int = 0
        protected set

    init {
        require(!endsOn.isBefore(startsOn)) { "종료일은 시작일보다 앞설 수 없습니다: $startsOn ~ $endsOn" }
        require(capacity > 0) { "정원은 1 이상이어야 합니다: $capacity" }
    }
}
