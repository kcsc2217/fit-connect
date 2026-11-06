package com.fitness.user.security

import java.time.Instant

/** access 검증 결과. 필터가 예외 없이 분기하도록 값으로 돌려준다 */
sealed interface AccessTokenResult {
    data class Valid(val userId: Long) : AccessTokenResult
    data object Expired : AccessTokenResult
    data object Invalid : AccessTokenResult
}

data class IssuedToken(val value: String, val expiresAt: Instant)
