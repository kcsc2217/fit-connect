package com.fitness.workout.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fitness.support.AuthTestSupport.loginAsDev
import com.fitness.support.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@IntegrationTest
class ExerciseControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    @Test
    fun `활성 종목을 부위와 id 순으로 돌려준다`() {
        // given
        val access = mockMvc.loginAsDev(objectMapper).accessToken

        // when
        val body = mockMvc.get("/api/v1/exercises") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $access")
        }.andReturn().response.getContentAsString(Charsets.UTF_8) // 응답에 charset이 없으면 ISO-8859-1로 읽혀 한글이 깨진다

        // then: 정렬 순서(부위 이름 → id)가 지켜진다
        val items = objectMapper.readTree(body).map { it["bodyPart"].asText() to it["id"].asLong() }
        assertThat(items).isNotEmpty.isSortedAccordingTo(compareBy<Pair<String, Long>>({ it.first }, { it.second }))
    }

    @Test
    fun `종목마다 앱이 고르고 휴식 타이머에 쓸 값을 담는다`() {
        // given
        val access = mockMvc.loginAsDev(objectMapper).accessToken

        // when
        val body = mockMvc.get("/api/v1/exercises") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $access")
        }.andReturn().response.getContentAsString(Charsets.UTF_8) // 응답에 charset이 없으면 ISO-8859-1로 읽혀 한글이 깨진다

        // then: 시드의 바벨 벤치 프레스 (docs/data/exercises.csv)
        val bench = objectMapper.readTree(body).first { it["code"].asText() == "BENCH_PRESS" }
        assertThat(
            listOf(bench["nameKo"].asText(), bench["bodyPart"].asText(), bench["loadType"].asText(), bench["defaultRestSec"].asInt()),
        ).isEqualTo(listOf("바벨 벤치 프레스", "CHEST", "BARBELL", 150))
    }
}
