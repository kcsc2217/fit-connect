package com.fitness.user.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RefreshTokenTest {

    @Test
    fun `점이 없거나 tokenId 형식이 틀린 값은 파싱하지 않는다`() {
        // given
        val validTokenId = "a".repeat(43)
        val values = listOf(
            "no-dot-at-all",
            "1.too-short",
            "abc.$validTokenId",          // userId가 숫자가 아님
            "1.$validTokenId.extra",      // 점이 두 개
            "1." + "a".repeat(42) + "!",  // 허용하지 않는 문자
        )

        // when
        val parsed = values.map { RefreshToken.parse(it) }

        // then
        assertThat(parsed).containsOnlyNulls()
    }

    @Test
    fun `문자열로 바꿔도 tokenId가 드러나지 않는다`() {
        // given
        val token = RefreshToken.issue(1L)

        // when
        val text = token.toString()

        // then
        assertThat(text).doesNotContain(token.tokenId)
    }
}
