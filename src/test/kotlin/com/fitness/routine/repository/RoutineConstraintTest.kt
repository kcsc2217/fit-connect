package com.fitness.routine.repository

import com.fitness.support.RepositoryTest
import com.fitness.support.RoutineFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException

/** 같은 날 루틴 중복 생성을 막는 (user_id, routine_date) 유니크와 routine 모듈 FK를 확인한다. */
@RepositoryTest
class RoutineConstraintTest @Autowired constructor(
    private val dailyRoutineRepository: DailyRoutineRepository,
    private val routineItemRepository: RoutineItemRepository,
) {

    private val userId = 1L

    @Test
    fun `같은 사용자가 같은 날짜에 루틴을 두 번 저장하면 유니크 제약 위반 예외가 난다`() {
        // given
        dailyRoutineRepository.saveAndFlush(RoutineFixtures.routine(userId))

        // when & then
        assertThrows<DataIntegrityViolationException> {
            dailyRoutineRepository.saveAndFlush(RoutineFixtures.routine(userId))
        }
    }

    @Test
    fun `다른 사용자는 같은 날짜에 루틴을 저장할 수 있다`() {
        // given
        val otherUserId = 2L
        dailyRoutineRepository.saveAndFlush(RoutineFixtures.routine(userId))

        // when
        val saved = dailyRoutineRepository.saveAndFlush(RoutineFixtures.routine(otherUserId))

        // then
        assertThat(saved.id).isNotNull
    }

    @Test
    fun `존재하지 않는 루틴 id로 항목을 저장하면 외래 키 위반 예외가 난다`() {
        // given
        val missingRoutineId = Long.MAX_VALUE

        // when & then
        assertThrows<DataIntegrityViolationException> {
            routineItemRepository.saveAndFlush(RoutineFixtures.item(missingRoutineId))
        }
    }
}
