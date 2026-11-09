package com.fitness.support

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID

/** 테스트용 RSA 키와 id_token. 기본값은 "카카오가 우리 앱(test-kakao-app-key)에 발급한 유효한 토큰" */
object IdTokens {
    const val KAKAO_ISSUER = "https://kauth.kakao.com"
    const val GOOGLE_ISSUER = "https://accounts.google.com"
    const val KAKAO_AUD = "test-kakao-app-key"
    const val GOOGLE_AUD = "test-google-client-id"

    fun rsaKey(kid: String = "kid-" + UUID.randomUUID()): RSAKey = RSAKeyGenerator(2048).keyID(kid).generate()

    fun sign(
        key: RSAKey,
        sub: String? = "sub-" + UUID.randomUUID(),
        iss: String = KAKAO_ISSUER,
        aud: String = KAKAO_AUD,
        exp: Instant? = Instant.now().plus(Duration.ofHours(1)),
    ): String {
        val claims = JWTClaimsSet.Builder().issuer(iss).audience(aud).issueTime(Date())
            .apply {
                sub?.let { subject(it) }
                exp?.let { expirationTime(Date.from(it)) }
            }
            .build()
        return SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.keyID).build(), claims)
            .apply { sign(RSASSASigner(key)) }
            .serialize()
    }

    /** 알고리즘 혼동 공격 흉내: RS256 대신 HS256으로 서명한 토큰 */
    fun signHs256(secret: ByteArray, kid: String): String =
        SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.HS256).keyID(kid).build(),
            JWTClaimsSet.Builder().issuer(KAKAO_ISSUER).audience(KAKAO_AUD).subject("x")
                .expirationTime(Date.from(Instant.now().plusSeconds(3600))).build(),
        ).apply { sign(MACSigner(secret)) }.serialize()
}
