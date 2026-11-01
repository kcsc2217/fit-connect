package com.fitness.workout.domain

import com.fitness.common.entity.BaseTimeEntity
import com.fitness.common.type.Equipment
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.io.Serializable

/**
 * 종목에 필요한 장비. 여러 장비가 필요하면 행이 여러 개다 (벤치프레스 = BARBELL + BENCH + RACK).
 * 맨몸 종목은 행이 없고, 장비와 관계없이 항상 후보가 된다.
 */
@Entity
@Table(name = "exercise_equipment")
class ExerciseEquipment(

    @EmbeddedId
    val id: ExerciseEquipmentId,

) : BaseTimeEntity()

@Embeddable
data class ExerciseEquipmentId(

    @Column(nullable = false)
    val exerciseId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val equipment: Equipment,

) : Serializable
