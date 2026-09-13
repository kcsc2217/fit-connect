package com.fitness.reservation.repository

import com.fitness.reservation.domain.Reservation
import org.springframework.data.jpa.repository.JpaRepository

interface ReservationRepository : JpaRepository<Reservation, Long> {
    fun findByUserIdAndPtClassId(userId: Long, ptClassId: Long): Reservation?
    fun countByPtClassId(ptClassId: Long): Long
}