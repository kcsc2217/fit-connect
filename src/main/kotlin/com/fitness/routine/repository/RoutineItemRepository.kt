package com.fitness.routine.repository

import com.fitness.routine.domain.RoutineItem
import org.springframework.data.jpa.repository.JpaRepository

interface RoutineItemRepository : JpaRepository<RoutineItem, Long>
