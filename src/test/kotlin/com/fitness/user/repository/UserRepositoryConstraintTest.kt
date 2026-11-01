package com.fitness.user.repository

import com.fitness.common.type.Equipment
import com.fitness.support.RepositoryTest
import com.fitness.support.UserFixtures
import com.fitness.user.domain.AuthProvider
import com.fitness.user.domain.DeviceToken
import com.fitness.user.domain.Platform
import com.fitness.user.domain.SocialAccount
import com.fitness.user.domain.UserEquipment
import com.fitness.user.domain.UserEquipmentId
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.dao.DataIntegrityViolationException
import java.time.Instant

/** user 모듈 마이그레이션의 유니크·외래 키·복합 키 제약이 실제로 동작하는지 확인한다. */
@RepositoryTest
class UserRepositoryConstraintTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository,
    private val userProfileRepository: UserProfileRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val entityManager: TestEntityManager,
) {

    @Test
    fun `같은 provider와 provider_user_id로 소셜 계정을 두 번 저장하면 유니크 제약 위반 예외가 난다`() {
        // given
        val first = userRepository.saveAndFlush(UserFixtures.user("첫번째")).id!!
        val second = userRepository.saveAndFlush(UserFixtures.user("두번째")).id!!
        socialAccountRepository.saveAndFlush(SocialAccount(first, AuthProvider.KAKAO, "kakao-123"))

        // when & then
        assertThrows<DataIntegrityViolationException> {
            socialAccountRepository.saveAndFlush(SocialAccount(second, AuthProvider.KAKAO, "kakao-123"))
        }
    }

    @Test
    fun `같은 FCM 토큰을 두 번 저장하면 유니크 제약 위반 예외가 난다`() {
        // given
        val userId = userRepository.saveAndFlush(UserFixtures.user()).id!!
        val now = Instant.now()
        deviceTokenRepository.saveAndFlush(DeviceToken(userId, "fcm-token-1", Platform.ANDROID, now))

        // when & then
        assertThrows<DataIntegrityViolationException> {
            deviceTokenRepository.saveAndFlush(DeviceToken(userId, "fcm-token-1", Platform.IOS, now))
        }
    }

    @Test
    fun `존재하지 않는 사용자 id로 프로필을 저장하면 외래 키 위반 예외가 난다`() {
        // given
        val missingUserId = Long.MAX_VALUE

        // when & then
        assertThrows<DataIntegrityViolationException> {
            userProfileRepository.saveAndFlush(UserFixtures.profile(missingUserId))
        }
    }

    @Test
    fun `같은 사용자에게 같은 장비를 중복 저장하면 PK 위반 예외가 난다`() {
        // given
        val userId = userRepository.saveAndFlush(UserFixtures.user()).id!!
        val id = UserEquipmentId(userId, Equipment.BARBELL)
        entityManager.persistAndFlush(UserEquipment(id))
        entityManager.clear() // 1차 캐시를 비워 merge가 아니라 실제 INSERT가 DB까지 가게 한다

        // when & then
        assertThrows<ConstraintViolationException> {
            entityManager.persistAndFlush(UserEquipment(id))
        }
    }
}
