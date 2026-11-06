package com.fitness.user.service

import com.fitness.common.exception.BusinessException
import com.fitness.common.exception.ErrorCode
import com.fitness.user.client.SocialIdentity
import com.fitness.user.client.SocialTokenVerifiers
import com.fitness.user.domain.RefreshToken
import com.fitness.user.dto.AccountInfo
import com.fitness.user.dto.LoginRequest
import com.fitness.user.dto.LoginResponse
import com.fitness.user.dto.TokenResponse
import com.fitness.user.repository.RefreshTokenRepository
import com.fitness.user.security.JwtProperties
import com.fitness.user.security.JwtProvider
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.time.Clock

/**
 * 로그인·갱신·로그아웃 (docs/decisions/0004-auth-token.md).
 * 트랜잭션을 걸지 않는다: 가입이 유니크 위반으로 롤백되면 새 트랜잭션에서 다시 조회해야 하기 때문이다 (계획 006 1-5).
 */
@Service
class AuthService(
    private val socialTokenVerifiers: SocialTokenVerifiers,
    private val userAccountService: UserAccountService,
    private val jwtProvider: JwtProvider,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProperties: JwtProperties,
    private val clock: Clock,
) {
    fun login(request: LoginRequest): LoginResponse {
        val identity = socialTokenVerifiers.verify(request.provider, request.token)
        val account = userAccountService.find(identity) ?: registerOrFind(identity)
        return LoginResponse.of(issueTokens(account.userId), account.status)
    }

    /** refresh를 꺼내면서 지우고(한 번만 쓸 수 있음) 새 access·refresh를 발급한다 */
    fun refresh(rawToken: String): TokenResponse {
        val token = RefreshToken.parse(rawToken) ?: throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)
        if (!refreshTokenRepository.consume(token)) {
            throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN) // 만료·로그아웃·이미 교체됨
        }
        return issueTokens(token.userId)
    }

    /** 이미 없거나 형식이 틀려도 성공으로 끝낸다 (멱등) */
    fun logout(rawToken: String) {
        val token = RefreshToken.parse(rawToken) ?: return
        refreshTokenRepository.consume(token)
    }

    // 같은 소셜 계정의 첫 로그인이 동시에 오면 한쪽은 유니크 위반으로 롤백된다. 그쪽은 먼저 만들어진 회원을 다시 읽는다
    private fun registerOrFind(identity: SocialIdentity): AccountInfo = try {
        userAccountService.register(identity)
    } catch (e: DataIntegrityViolationException) {
        userAccountService.find(identity) ?: throw e // 유니크가 아닌 다른 제약 위반이면 그대로 500
    }

    private fun issueTokens(userId: Long): TokenResponse {
        val access = jwtProvider.issueAccessToken(userId)
        val refresh = RefreshToken.issue(userId)
        refreshTokenRepository.save(refresh, jwtProperties.refreshTtl)
        return TokenResponse(
            accessToken = access.value,
            accessTokenExpiresAt = access.expiresAt,
            refreshToken = refresh.value,
            refreshTokenExpiresAt = clock.instant().plus(jwtProperties.refreshTtl),
        )
    }
}
