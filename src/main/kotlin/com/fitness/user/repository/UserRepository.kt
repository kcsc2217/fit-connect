package com.fitness.user.repository

import com.fitness.user.domain.AuthProvider
import com.fitness.user.domain.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByProviderAndProviderId(provider: AuthProvider, providerId: String): User?
}