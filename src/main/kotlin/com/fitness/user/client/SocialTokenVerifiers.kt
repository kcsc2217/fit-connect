package com.fitness.user.client

import com.fitness.common.exception.BusinessException
import com.fitness.common.exception.ErrorCode
import com.fitness.user.domain.AuthProvider
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/** provider에 맞는 신원 확인기를 고른다. 확인기가 없으면 400 UNSUPPORTED_PROVIDER */
@Component
class SocialTokenVerifiers(verifiers: ObjectProvider<SocialTokenVerifier>) {

    // 1-A 운영에는 확인기가 하나도 없다. 빈이 0개여도 시작할 수 있게 ObjectProvider로 받는다
    private val byProvider: Map<AuthProvider, SocialTokenVerifier> =
        verifiers.orderedStream().toList().associateBy { it.provider }

    fun verify(provider: AuthProvider, token: String): SocialIdentity {
        val verifier = byProvider[provider]
            ?: throw BusinessException(ErrorCode.UNSUPPORTED_PROVIDER, "지원하지 않는 로그인 방식입니다: $provider")
        return verifier.verify(token)
    }
}
