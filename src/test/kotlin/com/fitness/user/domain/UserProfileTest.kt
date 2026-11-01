package com.fitness.user.domain

import com.fitness.support.UserFixtures
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserProfileTest {

    @Test
    fun `요일 비트마스크가 127을 넘으면 IllegalArgumentException을 던진다`() {
        // given
        val profile = UserFixtures.profile(userId = 1L)

        // when & then
        assertThrows<IllegalArgumentException> {
            profile.workoutDays = 128 // TINYINT라 검증이 없으면 -128로 저장된다
        }
    }

    @Test
    fun `요일 비트마스크가 음수면 생성할 때 IllegalArgumentException을 던진다`() {
        // given
        val invalid = -1

        // when & then
        assertThrows<IllegalArgumentException> {
            UserFixtures.profile(userId = 1L, workoutDays = invalid)
        }
    }
}
