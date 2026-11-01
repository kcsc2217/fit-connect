package com.fitness.user.repository

import com.fitness.user.domain.DeviceToken
import org.springframework.data.jpa.repository.JpaRepository

interface DeviceTokenRepository : JpaRepository<DeviceToken, Long> {
    fun findByToken(token: String): DeviceToken?
    fun findAllByUserId(userId: Long): List<DeviceToken>
}
