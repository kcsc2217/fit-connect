package com.fitness.challenge.repository

import com.fitness.challenge.domain.Challenge
import org.springframework.data.jpa.repository.JpaRepository

interface ChallengeRepository : JpaRepository<Challenge, Long>
