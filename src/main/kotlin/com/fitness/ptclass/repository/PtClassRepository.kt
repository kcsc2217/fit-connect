package com.fitness.ptclass.repository

import com.fitness.ptclass.domain.PtClass
import org.springframework.data.jpa.repository.JpaRepository

interface PtClassRepository : JpaRepository<PtClass, Long> {
    fun findByTrainerId(trainerId: Long): List<PtClass>
}