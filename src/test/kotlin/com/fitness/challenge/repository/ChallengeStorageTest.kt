package com.fitness.challenge.repository

import com.fitness.support.ChallengeFixtures
import com.fitness.support.RepositoryTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant

/** 챌린지 시각 컬럼이 DB에 UTC로 들어가는지 원본 값을 직접 읽어 확인한다. */
@RepositoryTest
class ChallengeStorageTest @Autowired constructor(
    private val challengeRepository: ChallengeRepository,
    private val jdbcTemplate: JdbcTemplate,
) {

    @Test
    fun `일반 신청 오픈 시각은 UTC로 저장된다`() {
        // given: KST로 밀려 저장되면 2026-11-02 08:00이 된다
        val challenge = ChallengeFixtures.challenge(openAt = Instant.parse("2026-11-01T23:00:00Z"))

        // when
        val saved = challengeRepository.saveAndFlush(challenge)

        // then
        val stored = jdbcTemplate.queryForObject(
            "SELECT DATE_FORMAT(open_at, '%Y-%m-%d %H:%i:%s.%f') FROM challenge WHERE id = ?",
            String::class.java,
            saved.id,
        )
        assertThat(stored).isEqualTo("2026-11-01 23:00:00.000000")
    }
}
