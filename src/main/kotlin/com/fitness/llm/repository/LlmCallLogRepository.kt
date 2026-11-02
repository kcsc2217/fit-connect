package com.fitness.llm.repository

import com.fitness.llm.domain.LlmCallLog
import org.springframework.data.jpa.repository.JpaRepository

interface LlmCallLogRepository : JpaRepository<LlmCallLog, Long>
