package com.fitness.user.client

import com.nimbusds.jose.KeySourceException
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.JWKSetCacheRefreshEvaluator
import com.nimbusds.jose.jwk.source.JWKSetSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.security.oauth2.jwt.JwtDecoder

/**
 * 공개키 받기의 마지막 결과(성공·실패)를 기록한다. Nimbus 공개키 소스의 가장 안쪽(실제 HTTP 받기)에 끼운다.
 * Nimbus는 실패한 받기도 다시 받기 제한 횟수로 세어, 장애 중에도 RateLimitReachedException이 난다.
 * 그때 "키가 정말 없음(401)"과 "공개키를 받을 수 없음(503)"을 이 기록으로 나눈다 (계획 007 리뷰 지적 1).
 */
class FailureTrackingJWKSetSource(
    private val delegate: JWKSetSource<SecurityContext>,
) : JWKSetSource<SecurityContext> {

    @Volatile
    var lastRetrievalFailed: Boolean = false
        private set

    override fun getJWKSet(
        refreshEvaluator: JWKSetCacheRefreshEvaluator,
        currentTime: Long,
        context: SecurityContext?,
    ): JWKSet = try {
        delegate.getJWKSet(refreshEvaluator, currentTime, context).also { lastRetrievalFailed = false }
    } catch (e: KeySourceException) {
        lastRetrievalFailed = true // 연결 실패, 타임아웃, 5xx, JSON 아님, 크기 초과 모두 여기로 온다
        throw e
    }

    override fun close() = delegate.close()
}

/** id_token decoder + 공개키 받기 상태. 확인기가 401·503을 나눌 때 상태를 본다 */
class OidcJwtDecoder(
    private val delegate: JwtDecoder,
    private val keySource: FailureTrackingJWKSetSource,
) : JwtDecoder by delegate {

    /** 마지막 공개키 받기가 실패했는지 (아직 받은 적이 없으면 false) */
    val lastKeyRetrievalFailed: Boolean get() = keySource.lastRetrievalFailed
}
