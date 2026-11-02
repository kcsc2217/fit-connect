package com.fitness.notification.repository

import com.fitness.notification.domain.PushType
import com.fitness.support.RepositoryTest
import com.fitness.support.RoutineFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant

/** 같은 대상에 알림이 두 번 가지 않게 하는 (user_id, type, ref_id) 유니크와 발송 시각 저장을 확인한다. */
@RepositoryTest
class PushLogConstraintTest @Autowired constructor(
    private val pushLogRepository: PushLogRepository,
    private val jdbcTemplate: JdbcTemplate,
) {

    private val userId = 1L

    @Test
    fun `같은 사용자에게 같은 종류·대상의 알림을 두 번 저장하면 유니크 제약 위반 예외가 난다`() {
        // given
        pushLogRepository.saveAndFlush(RoutineFixtures.push(userId, refId = 5L))

        // when & then
        assertThrows<DataIntegrityViolationException> {
            pushLogRepository.saveAndFlush(RoutineFixtures.push(userId, refId = 5L))
        }
    }

    @Test
    fun `종류가 다르면 같은 대상 id로 알림을 저장할 수 있다`() {
        // given: 루틴 5번과 챌린지 5번은 서로 다른 대상
        pushLogRepository.saveAndFlush(RoutineFixtures.push(userId, refId = 5L, type = PushType.DAILY_ROUTINE))

        // when
        val saved = pushLogRepository.saveAndFlush(
            RoutineFixtures.push(userId, refId = 5L, type = PushType.CHALLENGE_PROMOTED),
        )

        // then
        assertThat(saved.id).isNotNull
    }

    @Test
    fun `발송 예정 시각은 UTC로 저장된다`() {
        // given: KST로 밀려 저장되면 2026-11-02 07:30이 된다
        val push = RoutineFixtures.push(userId, refId = 5L, scheduledAt = Instant.parse("2026-11-01T22:30:00Z"))

        // when
        val saved = pushLogRepository.saveAndFlush(push)

        // then
        val stored = jdbcTemplate.queryForObject(
            "SELECT DATE_FORMAT(scheduled_at, '%Y-%m-%d %H:%i:%s.%f') FROM push_log WHERE id = ?",
            String::class.java,
            saved.id,
        )
        assertThat(stored).isEqualTo("2026-11-01 22:30:00.000000")
    }
}
