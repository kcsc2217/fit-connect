package com.fitness.user.client

import com.fitness.common.exception.BusinessException
import com.fitness.common.exception.ErrorCode
import com.fitness.user.domain.AuthProvider
import com.nimbusds.jose.jwk.source.RateLimitReachedException
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.JwtException

/** 카카오·구글 id_token 확인기 (ADR 0005). provider별 차이(iss, 공개키 주소, aud)는 decoder 설정에 들어 있다 */
class OidcIdTokenVerifier(
    override val provider: AuthProvider,
    private val decoder: OidcJwtDecoder,
) : SocialTokenVerifier {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun verify(token: String): SocialIdentity {
        val jwt = try {
            decoder.decode(token)
        } catch (e: BadJwtException) {
            // 서명·형식·iss·aud·exp·sub 문제 (JwtValidationException도 BadJwtException이다)
            throw BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN)
        } catch (e: JwtException) {
            // 다시 받기 제한에 걸렸고 마지막 받기가 성공했다 = 최신 키를 받았는데도 그 kid가 없음 → 토큰 문제.
            // 마지막 받기가 실패했다면 장애 중 제한에 걸린 것이라 503이다 (Nimbus는 실패한 받기도 제한 횟수로 센다)
            if (e.hasCause<RateLimitReachedException>() && !decoder.lastKeyRetrievalFailed) {
                throw BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN)
            }
            // 그 밖의 JwtException = 공개키를 받지 못함 (회사 쪽 장애·타임아웃). 토큰 원문은 남기지 않는다
            log.warn("{} 공개키를 가져오지 못했습니다: {}", provider, e.message)
            throw BusinessException(ErrorCode.SOCIAL_LOGIN_UNAVAILABLE)
        }
        // sub 형식은 decoder의 검증 규칙이 이미 확인했다. 이메일은 저장하지 않는다 (명세)
        return SocialIdentity(provider, jwt.subject, email = null)
    }

    private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean =
        generateSequence(this as Throwable?) { it.cause }.any { it is T }
}
