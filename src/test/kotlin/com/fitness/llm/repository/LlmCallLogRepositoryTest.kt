package com.fitness.llm.repository

import com.fitness.support.RepositoryTest
import com.fitness.support.RoutineFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@RepositoryTest
class LlmCallLogRepositoryTest @Autowired constructor(
    private val llmCallLogRepository: LlmCallLogRepository,
    private val jdbcTemplate: JdbcTemplate,
) {

    @Test
    fun `평가 실행 로그는 사용자 없이 저장할 수 있다`() {
        // given
        val evalLog = RoutineFixtures.llmCall(userId = null, refId = null)

        // when
        val saved = llmCallLogRepository.saveAndFlush(evalLog)

        // then
        // queryForObject의 Kotlin 확장은 null을 돌려줄 수 없어 DB에서 NULL 여부를 계산해 읽는다
        val userIdIsNull = jdbcTemplate.queryForObject(
            "SELECT user_id IS NULL FROM llm_call_log WHERE id = ?",
            Boolean::class.java,
            saved.id,
        )
        assertThat(userIdIsNull).isTrue()
    }
}
