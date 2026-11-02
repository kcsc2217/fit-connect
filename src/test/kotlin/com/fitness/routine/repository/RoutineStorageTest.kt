package com.fitness.routine.repository

import com.fitness.support.RepositoryTest
import com.fitness.support.RoutineFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate

/** 사용자 기준 날짜(DATE)가 DB에 의도대로 들어가는지 원본 값을 직접 읽어 확인한다. */
@RepositoryTest
class RoutineStorageTest @Autowired constructor(
    private val dailyRoutineRepository: DailyRoutineRepository,
    private val jdbcTemplate: JdbcTemplate,
) {

    @Test
    fun `루틴 날짜는 시간대 변환 없이 저장된다`() {
        // given: 테스트 JVM은 Asia/Seoul. 변환되면 하루 전(2026-11-01)으로 밀린다
        val routine = RoutineFixtures.routine(userId = 1L, routineDate = LocalDate.of(2026, 11, 2))

        // when
        val saved = dailyRoutineRepository.saveAndFlush(routine)

        // then
        val stored = jdbcTemplate.queryForObject(
            "SELECT DATE_FORMAT(routine_date, '%Y-%m-%d') FROM daily_routine WHERE id = ?",
            String::class.java,
            saved.id,
        )
        assertThat(stored).isEqualTo("2026-11-02")
    }
}
