package com.fitness.workout.repository

import com.fitness.workout.domain.ExerciseEquipment
import com.fitness.workout.domain.ExerciseEquipmentId
import org.springframework.data.jpa.repository.JpaRepository

interface ExerciseEquipmentRepository : JpaRepository<ExerciseEquipment, ExerciseEquipmentId>
