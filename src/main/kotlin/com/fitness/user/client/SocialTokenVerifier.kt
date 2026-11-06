package com.fitness.user.client

import com.fitness.user.domain.AuthProvider

/** 소셜 토큰으로 신원을 확인한다. 1-B에서 카카오·구글 구현을 추가한다 */
interface SocialTokenVerifier {
    val provider: AuthProvider

    /** 토큰이 유효하면 신원을, 아니면 INVALID_SOCIAL_TOKEN BusinessException */
    fun verify(token: String): SocialIdentity
}

data class SocialIdentity(
    val provider: AuthProvider,
    val providerUserId: String,
    val email: String?,
)
