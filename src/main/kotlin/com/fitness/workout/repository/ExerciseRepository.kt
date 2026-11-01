package com.fitness.workout.repository

import com.fitness.common.type.Equipment
import com.fitness.workout.domain.BodyPart
import com.fitness.workout.domain.Exercise
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ExerciseRepository : JpaRepository<Exercise, Long> {

    fun findByCode(code: String): Exercise?

    // 필요한 장비를 모두 가진 활성 종목 (필요 장비가 없는 맨몸 종목 포함). equipments는 비어 있으면 안 된다
    @Query(
        """
        SELECT e FROM Exercise e
        WHERE e.active = true AND e.bodyPart IN :bodyParts
          AND NOT EXISTS (
            SELECT 1 FROM ExerciseEquipment ee
            WHERE ee.id.exerciseId = e.id AND ee.id.equipment NOT IN :equipments
          )
        """,
    )
    fun findActiveOwning(bodyParts: Collection<BodyPart>, equipments: Collection<Equipment>): List<Exercise>

    // 필요 장비가 없는 활성 종목 (맨몸)
    @Query(
        """
        SELECT e FROM Exercise e
        WHERE e.active = true AND e.bodyPart IN :bodyParts
          AND NOT EXISTS (SELECT 1 FROM ExerciseEquipment ee WHERE ee.id.exerciseId = e.id)
        """,
    )
    fun findActiveBodyweight(bodyParts: Collection<BodyPart>): List<Exercise>
}
