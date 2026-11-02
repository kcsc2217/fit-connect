package com.fitness.analytics.repository

import com.fitness.analytics.domain.EventLog
import org.springframework.data.jpa.repository.JpaRepository

interface EventLogRepository : JpaRepository<EventLog, Long>
