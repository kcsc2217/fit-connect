package com.fitness.user.repository

import com.fitness.user.domain.AuthProvider
import com.fitness.user.domain.SocialAccount
import org.springframework.data.jpa.repository.JpaRepository

interface SocialAccountRepository : JpaRepository<SocialAccount, Long> {
    fun findByProviderAndProviderUserId(provider: AuthProvider, providerUserId: String): SocialAccount?
}
