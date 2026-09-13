package com.fitness.diet.repository

import com.fitness.diet.domain.DietLog
import org.springframework.data.jpa.repository.JpaRepository

interface DietLogRepository : JpaRepository<DietLog, Long> {
    fun findByUserId(userId: Long): List<DietLog>
}