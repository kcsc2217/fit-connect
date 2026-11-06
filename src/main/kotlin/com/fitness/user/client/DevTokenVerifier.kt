package com.fitness.user.client

import com.fitness.common.exception.BusinessException
import com.fitness.common.exception.ErrorCode
import com.fitness.user.domain.AuthProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * 개발·테스트용: token 값을 그대로 소셜 사용자 ID로 쓴다.
 * 설정 fitness.auth.dev-login.enabled=true일 때만 빈이 생긴다 (application-local.yml, application-test.yml에만 있음).
 * 프로파일 조건을 쓰지 않는 이유: 기본 활성 프로파일이 local이라 운영에서 프로파일 지정을 빠뜨리면 열린다 (계획 006 1-6).
 */
@Component
@ConditionalOnProperty("fitness.auth.dev-login.enabled", havingValue = "true")
class DevTokenVerifier : SocialTokenVerifier {
    override val provider = AuthProvider.DEV

    override fun verify(token: String): SocialIdentity {
        if (token.isBlank() || token.length > MAX_ID_LENGTH) throw BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN)
        return SocialIdentity(AuthProvider.DEV, token, email = null)
    }

    companion object {
        private const val MAX_ID_LENGTH = 100 // social_account.provider_user_id VARCHAR(100)
    }
}
