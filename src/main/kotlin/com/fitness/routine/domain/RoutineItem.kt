package com.fitness.routine.domain

import com.fitness.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal

/** 루틴 항목. 세트·반복·무게는 서버 규칙 계산값이고, LLM은 종목 구성·순서만 정한다. */
@Entity
@Table(
    name = "routine_item",
    indexes = [Index(name = "idx_routine_item_routine_order", columnList = "routine_id, item_order")],
)
class RoutineItem(

    // 같은 모듈이라 FK 있음 (DDL)
    @Column(nullable = false)
    val routineId: Long,

    // workout 모듈 소유라 FK 없음. 존재 여부는 생성 시 후보 종목으로 검증
    @Column(nullable = false)
    val exerciseId: Long,

    @Column(nullable = false)
    var itemOrder: Short,

    @Column(nullable = false)
    var sets: Short,

    @Column(nullable = false)
    var reps: Short,

    @Column(nullable = false, precision = 6, scale = 2)
    var targetWeightKg: BigDecimal,

    @Column(nullable = false)
    var restSec: Short,

    // 장비가 없을 때 대체 종목 (FK 없음)
    var alternativeExerciseId: Long? = null,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}
