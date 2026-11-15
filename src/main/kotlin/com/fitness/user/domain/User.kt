package com.fitness.user.domain

import com.fitness.common.entity.BaseTimeEntity
import com.fitness.common.exception.BusinessException
import com.fitness.common.exception.ErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "users")
class User(

    @Column(nullable = false, length = 30)
    var nickname: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: UserStatus = UserStatus.ONBOARDING,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    var withdrawnAt: Instant? = null
        protected set

    /** 온보딩 제출: ONBOARDING이면 ACTIVE, ACTIVE면 그대로(수정), 탈퇴 회원이면 FORBIDDEN (계획 009) */
    fun completeOnboarding() {
        when (status) {
            UserStatus.ONBOARDING -> status = UserStatus.ACTIVE
            UserStatus.ACTIVE -> Unit // 다시 제출 = 수정 (FR-04가 같은 API를 쓴다)
            UserStatus.WITHDRAWN -> throw BusinessException(ErrorCode.FORBIDDEN, "탈퇴한 회원입니다")
        }
    }

    fun withdraw(now: Instant) {
        status = UserStatus.WITHDRAWN
        withdrawnAt = now
    }
}
