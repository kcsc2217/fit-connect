package com.fitness.notification.repository

import com.fitness.notification.domain.PushLog
import org.springframework.data.jpa.repository.JpaRepository

interface PushLogRepository : JpaRepository<PushLog, Long>
