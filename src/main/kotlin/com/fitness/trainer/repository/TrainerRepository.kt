package com.fitness.trainer.repository

import com.fitness.trainer.domain.Trainer
import org.springframework.data.jpa.repository.JpaRepository

interface TrainerRepository : JpaRepository<Trainer, Long> {
    fun findByUserId(userId: Long): Trainer?
}