package com.fitness.analytics.repository

import com.fitness.support.ChallengeFixtures
import com.fitness.support.RepositoryTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@RepositoryTest
class EventLogStorageTest @Autowired constructor(
    private val eventLogRepository: EventLogRepository,
    private val jdbcTemplate: JdbcTemplate,
) {

    @Test
    fun `이벤트 속성은 JSON으로 저장된다`() {
        // given
        val event = ChallengeFixtures.event(properties = mapOf("routineId" to 5))

        // when
        val saved = eventLogRepository.saveAndFlush(event)

        // then: 문자열이 아니라 JSON 객체로 들어가야 DB에서 키로 꺼낼 수 있다
        val routineId = jdbcTemplate.queryForObject(
            "SELECT JSON_EXTRACT(properties, '$.routineId') FROM event_log WHERE id = ?",
            String::class.java,
            saved.id,
        )
        assertThat(routineId).isEqualTo("5")
    }
}
