package com.fitness.routine.repository

import com.fitness.routine.domain.DailyRoutine
import org.springframework.data.jpa.repository.JpaRepository

interface DailyRoutineRepository : JpaRepository<DailyRoutine, Long>
