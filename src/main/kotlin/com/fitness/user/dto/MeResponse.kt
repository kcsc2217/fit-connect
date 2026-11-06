package com.fitness.user.dto

import com.fitness.user.domain.AuthProvider
import com.fitness.user.domain.User
import com.fitness.user.domain.UserStatus
import java.time.Instant

data class MeResponse(
    val id: Long,
    val nickname: String,
    val status: UserStatus,
    val createdAt: Instant,
    val providers: List<AuthProvider>,
) {
    companion object {
        fun of(user: User, providers: List<AuthProvider>) =
            MeResponse(user.id!!, user.nickname, user.status, user.createdAt!!, providers)
    }
}
