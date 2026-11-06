package com.fitness.user.service

import com.fitness.common.exception.BusinessException
import com.fitness.common.exception.ErrorCode
import com.fitness.user.dto.MeResponse
import com.fitness.user.repository.SocialAccountRepository
import com.fitness.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository,
) {
    @Transactional(readOnly = true)
    fun getMe(userId: Long): MeResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND, "회원을 찾을 수 없습니다")
        val providers = socialAccountRepository.findAllByUserId(userId).map { it.provider }
        return MeResponse.of(user, providers)
    }
}
