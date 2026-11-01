package com.fitness.workout.domain

import com.fitness.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant

/** 종목별 개인 최고 기록. 세션 종료 이벤트로 갱신하는 사전 집계다. */
@Entity
@Table(name = "personal_record")
class PersonalRecord(

    @EmbeddedId
    val id: PersonalRecordId,

    @Column(nullable = false, precision = 6, scale = 2)
    var bestWeightKg: BigDecimal,

    // 추정 1RM. 이름 규칙대로면 "est1rm"이 되므로 컬럼명을 명시한다
    @Column(name = "est_1rm", nullable = false, precision = 6, scale = 2)
    var est1rm: BigDecimal,

    // 기록을 만든 세트
    @Column(nullable = false)
    var sourceSetId: Long,

    @Column(nullable = false)
    var achievedAt: Instant,

) : BaseTimeEntity()

@Embeddable
data class PersonalRecordId(

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val exerciseId: Long,

) : Serializable
