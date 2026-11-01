package com.fitness.workout.repository

import com.fitness.support.RepositoryTest
import com.fitness.support.WorkoutFixtures
import com.fitness.workout.domain.BodyPart
import com.fitness.workout.domain.WeeklyVolume
import com.fitness.workout.domain.WeeklyVolumeId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/** 새로 쓰는 타입(BINARY(16), DECIMAL, DATE)이 DB에 의도대로 들어가는지 원본 값을 직접 읽어 확인한다. */
@RepositoryTest
class WorkoutStorageTest @Autowired constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val workoutSetRepository: WorkoutSetRepository,
    private val weeklyVolumeRepository: WeeklyVolumeRepository,
    private val jdbcTemplate: JdbcTemplate,
) {

    private val userId = 1L

    @Test
    fun `client_id는 UUID 16바이트 그대로 저장된다`() {
        // given
        val clientId = UUID.fromString("0192f5a1-7c3e-7d4b-9a2f-3e5d6c7b8a91")
        val session = WorkoutFixtures.session(userId, clientId)

        // when
        val saved = workoutSessionRepository.saveAndFlush(session)

        // then
        val stored = jdbcTemplate.queryForObject(
            "SELECT HEX(client_id) FROM workout_session WHERE id = ?",
            String::class.java,
            saved.id,
        )
        assertThat(stored).isEqualTo("0192F5A17C3E7D4B9A2F3E5D6C7B8A91")
    }

    @Test
    fun `무게는 소수 둘째 자리까지 오차 없이 저장된다`() {
        // given
        val exerciseId = exerciseRepository.saveAndFlush(WorkoutFixtures.exercise("TEST_WEIGHT")).id!!
        val sessionId = workoutSessionRepository.saveAndFlush(WorkoutFixtures.session(userId)).id!!
        val set = WorkoutFixtures.set(userId, sessionId, exerciseId, weightKg = BigDecimal("82.5"))

        // when
        val saved = workoutSetRepository.saveAndFlush(set)

        // then
        val stored = jdbcTemplate.queryForObject(
            "SELECT CAST(weight_kg AS CHAR) FROM workout_set WHERE id = ?",
            String::class.java,
            saved.id,
        )
        assertThat(stored).isEqualTo("82.50")
    }

    @Test
    fun `주 시작일은 시간대 변환 없이 저장된다`() {
        // given: 테스트 JVM은 Asia/Seoul. 변환되면 하루 전(2026-11-01)으로 밀린다
        val id = WeeklyVolumeId(userId, LocalDate.of(2026, 11, 2), BodyPart.CHEST)

        // when
        weeklyVolumeRepository.saveAndFlush(WeeklyVolume(id, BigDecimal("4200.00")))

        // then
        val stored = jdbcTemplate.queryForObject(
            "SELECT DATE_FORMAT(week_start, '%Y-%m-%d') FROM weekly_volume WHERE user_id = ?",
            String::class.java,
            userId,
        )
        assertThat(stored).isEqualTo("2026-11-02")
    }
}
