package com.fitness.challenge.domain

import com.fitness.support.ChallengeFixtures
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class ChallengeTest {

    @Test
    fun `종료일이 시작일보다 앞서면 IllegalArgumentException을 던진다`() {
        // given
        val startsOn = LocalDate.of(2026, 12, 14)
        val endsOn = LocalDate.of(2026, 12, 1)

        // when & then
        assertThrows<IllegalArgumentException> {
            ChallengeFixtures.challenge(startsOn = startsOn, endsOn = endsOn)
        }
    }
}
