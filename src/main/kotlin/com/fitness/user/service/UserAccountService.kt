package com.fitness.user.service

import com.fitness.user.client.SocialIdentity
import com.fitness.user.domain.SocialAccount
import com.fitness.user.domain.User
import com.fitness.user.dto.AccountInfo
import com.fitness.user.repository.SocialAccountRepository
import com.fitness.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

/** 소셜 계정으로 회원을 찾고 가입시킨다. 두 메서드는 각자 트랜잭션이다 (호출하는 AuthService에는 트랜잭션이 없다) */
@Service
class UserAccountService(
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository,
) {
    @Transactional(readOnly = true)
    fun find(identity: SocialIdentity): AccountInfo? {
        val account = socialAccountRepository.findByProviderAndProviderUserId(identity.provider, identity.providerUserId)
            ?: return null
        val user = userRepository.findByIdOrNull(account.userId)
            ?: error("social_account(${account.id})의 회원이 없습니다") // FK가 있어 생기면 안 되는 상황
        return AccountInfo(user.id!!, user.status)
    }

    /** users와 social_account를 한 트랜잭션으로 만든다. 유니크 위반이면 둘 다 롤백된다 */
    @Transactional
    fun register(identity: SocialIdentity): AccountInfo {
        val user = userRepository.save(User(nickname = defaultNickname()))
        // flush로 유니크 위반을 커밋 시점이 아니라 여기서 드러낸다
        socialAccountRepository.saveAndFlush(
            SocialAccount(
                userId = user.id!!,
                provider = identity.provider,
                providerUserId = identity.providerUserId,
                email = identity.email,
            ),
        )
        return AccountInfo(user.id!!, user.status)
    }

    // 겹쳐도 된다 (닉네임에 유니크 제약 없음). 바꾸는 기능은 온보딩·설정에서
    private fun defaultNickname() = "회원" + Random.nextInt(100_000, 1_000_000)
}
