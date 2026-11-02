package com.fitness.challenge.repository

import com.fitness.support.ChallengeFixtures
import com.fitness.support.RepositoryTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.orm.ObjectOptimisticLockingFailureException

/** 동시성 실험의 전제인 중복 신청 유니크, challenge 모듈 FK, 낙관적 락 버전을 확인한다. */
@RepositoryTest
class ChallengeConstraintTest @Autowired constructor(
    private val challengeRepository: ChallengeRepository,
    private val participationRepository: ChallengeParticipationRepository,
    private val jdbcTemplate: JdbcTemplate,
) {

    private val userId = 1L

    @Test
    fun `같은 사용자가 같은 챌린지에 신청 행을 두 번 만들면 유니크 제약 위반 예외가 난다`() {
        // given
        val challengeId = challengeRepository.saveAndFlush(ChallengeFixtures.challenge()).id!!
        participationRepository.saveAndFlush(ChallengeFixtures.participation(challengeId, userId))

        // when & then
        assertThrows<DataIntegrityViolationException> {
            participationRepository.saveAndFlush(ChallengeFixtures.participation(challengeId, userId))
        }
    }

    @Test
    fun `다른 사용자는 같은 챌린지에 신청할 수 있다`() {
        // given
        val otherUserId = 2L
        val challengeId = challengeRepository.saveAndFlush(ChallengeFixtures.challenge()).id!!
        participationRepository.saveAndFlush(ChallengeFixtures.participation(challengeId, userId))

        // when
        val saved = participationRepository.saveAndFlush(ChallengeFixtures.participation(challengeId, otherUserId))

        // then
        assertThat(saved.id).isNotNull
    }

    @Test
    fun `존재하지 않는 챌린지 id로 신청하면 외래 키 위반 예외가 난다`() {
        // given
        val missingChallengeId = Long.MAX_VALUE

        // when & then
        assertThrows<DataIntegrityViolationException> {
            participationRepository.saveAndFlush(ChallengeFixtures.participation(missingChallengeId, userId))
        }
    }

    @Test
    fun `다른 곳에서 먼저 수정된 챌린지를 저장하면 낙관적 락 예외가 난다`() {
        // given: 읽어 둔 엔티티는 version 0, 다른 요청이 먼저 갱신해 DB는 version 1
        val challenge = challengeRepository.saveAndFlush(ChallengeFixtures.challenge())
        jdbcTemplate.update("UPDATE challenge SET version = version + 1 WHERE id = ?", challenge.id)
        challenge.appliedCount += 1

        // when & then
        assertThrows<ObjectOptimisticLockingFailureException> {
            challengeRepository.saveAndFlush(challenge)
        }
    }
}
