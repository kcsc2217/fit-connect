package com.fitness.user.repository

import com.fitness.user.domain.UserProfile
import org.springframework.data.jpa.repository.JpaRepository

interface UserProfileRepository : JpaRepository<UserProfile, Long>
