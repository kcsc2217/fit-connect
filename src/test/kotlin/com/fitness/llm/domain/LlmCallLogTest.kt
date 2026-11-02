package com.fitness.llm.domain

import com.fitness.support.RoutineFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LlmCallLogTest {

    @Test
    fun `실패 사유가 300자를 넘으면 300자로 잘라 둔다`() {
        // given
        val longReason = "x".repeat(LlmCallLog.FAILURE_REASON_MAX + 50)

        // when
        val log = RoutineFixtures.llmCall(failureReason = longReason)

        // then
        assertThat(log.failureReason).hasSize(LlmCallLog.FAILURE_REASON_MAX)
    }

    @Test
    fun `실패 사유를 자르는 위치가 이모지 중간이면 이모지 앞에서 자른다`() {
        // given: 이모지(서로게이트 쌍)가 299~300번째 코드 유닛에 걸친다
        val reason = "a".repeat(LlmCallLog.FAILURE_REASON_MAX - 1) + "😀"

        // when
        val log = RoutineFixtures.llmCall(failureReason = reason)

        // then: 짝 없는 서로게이트가 남으면 DB에 깨진 글자로 저장된다
        assertThat(log.failureReason).isEqualTo("a".repeat(LlmCallLog.FAILURE_REASON_MAX - 1))
    }
}
