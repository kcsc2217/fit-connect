package com.fitness.user.domain

import com.fitness.common.exception.BusinessException
import com.fitness.common.exception.ErrorCode
import com.fitness.support.UserFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class UserTest {

    @Test
    fun `ONBOARDING 회원이 온보딩을 마치면 ACTIVE가 된다`() {
        // given
        val user = UserFixtures.user()

        // when
        user.completeOnboarding()

        // then
        assertThat(user.status).isEqualTo(UserStatus.ACTIVE)
    }

    @Test
    fun `탈퇴한 회원이 온보딩을 마치려 하면 FORBIDDEN 예외를 던진다`() {
        // given
        val user = UserFixtures.user().apply { withdraw(Instant.parse("2026-11-15T00:00:00Z")) }

        // when
        val e = assertThrows<BusinessException> { user.completeOnboarding() }

        // then
        assertThat(e.errorCode).isEqualTo(ErrorCode.FORBIDDEN)
    }
}
