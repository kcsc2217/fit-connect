package com.fitness.workout.domain

import com.fitness.common.entity.BaseTimeEntity
import com.fitness.common.type.Experience
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

/** 종목 사전. 초기 데이터는 free-exercise-db에서 고른 종목(docs/data/exercises.csv)이다. */
@Entity
@Table(
    name = "exercise",
    // LLM 출력과 평가가 code로 종목을 찾는다
    uniqueConstraints = [UniqueConstraint(name = "uk_exercise_code", columnNames = ["code"])],
    indexes = [Index(name = "idx_exercise_body_part_active", columnList = "body_part, active")],
)
class Exercise(

    @Column(nullable = false, length = 50)
    val code: String,

    @Column(nullable = false, length = 50)
    var nameKo: String,

    @Column(nullable = false, length = 100)
    var nameEn: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var bodyPart: BodyPart,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var loadType: LoadType,

    // 권장 최소 경력
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var minLevel: Experience,

    // 다관절 운동 여부 (순서 배치용)
    @Column(nullable = false)
    var compound: Boolean,

    @Column(nullable = false)
    var defaultRestSec: Short,

    // 비활성 종목은 후보에서 제외
    @Column(nullable = false)
    var active: Boolean = true,

    // free-exercise-db 원본 ID (자세 설명·이미지 연결용)
    @Column(length = 100)
    var sourceId: String? = null,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}
