package com.fitness.user.repository

import com.fitness.user.domain.UserEquipment
import com.fitness.user.domain.UserEquipmentId
import org.springframework.data.jpa.repository.JpaRepository

interface UserEquipmentRepository : JpaRepository<UserEquipment, UserEquipmentId> {
    fun findAllByIdUserId(userId: Long): List<UserEquipment>
}
