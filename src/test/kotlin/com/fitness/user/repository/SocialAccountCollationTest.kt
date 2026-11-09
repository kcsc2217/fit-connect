package com.fitness.user.repository

import com.fitness.support.RepositoryTest
import com.fitness.support.UserFixtures
import com.fitness.user.domain.AuthProvider
import com.fitness.user.domain.SocialAccount
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/** provider_user_id(id_token sub)는 대소문자·끝 공백을 구분한다 (V6, 계획 007 1-4) */
@RepositoryTest
class SocialAccountCollationTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository,
    private val entityManager: EntityManager,
) {

    private fun newUserId(): Long = userRepository.saveAndFlush(UserFixtures.user()).id!!

    @Test
    fun `provider_user_id 컬럼은 utf8mb4_0900_bin 콜레이션이다`() {
        // given
        val sql = """
            SELECT collation_name FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'social_account' AND column_name = 'provider_user_id'
        """.trimIndent()

        // when
        val collation = entityManager.createNativeQuery(sql).singleResult

        // then
        assertThat(collation).isEqualTo("utf8mb4_0900_bin")
    }

    @Test
    fun `대소문자만 다른 소셜 ID는 서로 다른 계정으로 저장된다`() {
        // given
        socialAccountRepository.saveAndFlush(SocialAccount(newUserId(), AuthProvider.KAKAO, "AbcDef"))
        val otherUserId = newUserId()

        // when
        socialAccountRepository.saveAndFlush(SocialAccount(otherUserId, AuthProvider.KAKAO, "abcdef"))

        // then
        assertThat(socialAccountRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, "abcdef")?.userId)
            .isEqualTo(otherUserId)
    }

    @Test
    fun `끝 공백만 다른 소셜 ID로 조회하면 찾지 못한다`() {
        // given
        socialAccountRepository.saveAndFlush(SocialAccount(newUserId(), AuthProvider.KAKAO, "trailing"))

        // when
        val found = socialAccountRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, "trailing ")

        // then
        assertThat(found).isNull()
    }
}
