package com.fitness.workout.repository

import com.fitness.support.RepositoryTest
import com.fitness.support.WorkoutFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.util.UUID

/** 오프라인 동기화 멱등성의 최후 안전망인 (user_id, client_id) 유니크와 workout 모듈 FK를 확인한다. */
@RepositoryTest
class WorkoutRecordConstraintTest @Autowired constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val workoutSetRepository: WorkoutSetRepository,
) {

    private val userId = 1L

    @Test
    fun `같은 사용자가 같은 client_id로 세트를 두 번 저장하면 유니크 제약 위반 예외가 난다`() {
        // given
        val exerciseId = exerciseRepository.saveAndFlush(WorkoutFixtures.exercise("TEST_SET_DUP")).id!!
        val sessionId = workoutSessionRepository.saveAndFlush(WorkoutFixtures.session(userId)).id!!
        val clientId = UUID.randomUUID()
        workoutSetRepository.saveAndFlush(WorkoutFixtures.set(userId, sessionId, exerciseId, clientId))

        // when & then
        assertThrows<DataIntegrityViolationException> {
            workoutSetRepository.saveAndFlush(WorkoutFixtures.set(userId, sessionId, exerciseId, clientId))
        }
    }

    @Test
    fun `다른 사용자는 같은 client_id로 세트를 저장할 수 있다`() {
        // given
        val otherUserId = 2L
        val exerciseId = exerciseRepository.saveAndFlush(WorkoutFixtures.exercise("TEST_SET_OTHER")).id!!
        val sessionId = workoutSessionRepository.saveAndFlush(WorkoutFixtures.session(userId)).id!!
        val otherSessionId = workoutSessionRepository.saveAndFlush(WorkoutFixtures.session(otherUserId)).id!!
        val clientId = UUID.randomUUID()
        workoutSetRepository.saveAndFlush(WorkoutFixtures.set(userId, sessionId, exerciseId, clientId))

        // when
        workoutSetRepository.saveAndFlush(WorkoutFixtures.set(otherUserId, otherSessionId, exerciseId, clientId))

        // then
        assertThat(workoutSetRepository.findByUserIdAndClientId(otherUserId, clientId)).isNotNull
    }

    @Test
    fun `같은 사용자가 같은 client_id로 세션을 두 번 저장하면 유니크 제약 위반 예외가 난다`() {
        // given
        val clientId = UUID.randomUUID()
        workoutSessionRepository.saveAndFlush(WorkoutFixtures.session(userId, clientId))

        // when & then
        assertThrows<DataIntegrityViolationException> {
            workoutSessionRepository.saveAndFlush(WorkoutFixtures.session(userId, clientId))
        }
    }

    @Test
    fun `존재하지 않는 세션 id로 세트를 저장하면 외래 키 위반 예외가 난다`() {
        // given
        val exerciseId = exerciseRepository.saveAndFlush(WorkoutFixtures.exercise("TEST_SET_NO_SESSION")).id!!
        val missingSessionId = Long.MAX_VALUE

        // when & then
        assertThrows<DataIntegrityViolationException> {
            workoutSetRepository.saveAndFlush(WorkoutFixtures.set(userId, missingSessionId, exerciseId))
        }
    }

    @Test
    fun `같은 종목 코드를 두 번 저장하면 유니크 제약 위반 예외가 난다`() {
        // given
        exerciseRepository.saveAndFlush(WorkoutFixtures.exercise("TEST_CODE_DUP"))

        // when & then
        assertThrows<DataIntegrityViolationException> {
            exerciseRepository.saveAndFlush(WorkoutFixtures.exercise("TEST_CODE_DUP"))
        }
    }
}
