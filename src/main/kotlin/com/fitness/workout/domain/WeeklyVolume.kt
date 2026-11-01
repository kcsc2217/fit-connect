package com.fitness.workout.domain

import com.fitness.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate

/** 부위별 주간 볼륨(무게 × 반복 수 합계). 세션 종료 이벤트로 갱신하는 사전 집계다. */
@Entity
@Table(name = "weekly_volume")
class WeeklyVolume(

    @EmbeddedId
    val id: WeeklyVolumeId,

    @Column(nullable = false, precision = 12, scale = 2)
    var volumeKg: BigDecimal,

) : BaseTimeEntity()

@Embeddable
data class WeeklyVolumeId(

    @Column(nullable = false)
    val userId: Long,

    // 사용자 시간대 기준 주 시작일(월요일)
    @Column(nullable = false)
    val weekStart: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val bodyPart: BodyPart,

) : Serializable
