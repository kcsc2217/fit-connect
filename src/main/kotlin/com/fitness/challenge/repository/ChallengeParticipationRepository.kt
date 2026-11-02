package com.fitness.challenge.repository

import com.fitness.challenge.domain.ChallengeParticipation
import org.springframework.data.jpa.repository.JpaRepository

interface ChallengeParticipationRepository : JpaRepository<ChallengeParticipation, Long>
