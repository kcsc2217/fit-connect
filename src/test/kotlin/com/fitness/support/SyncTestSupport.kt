package com.fitness.support

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fitness.workout.dto.SessionChange
import com.fitness.workout.dto.SetChange
import com.fitness.workout.dto.SyncRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** 운동 기록 동기화 테스트 도우미 (계획 011). 테스트마다 새 UUID를 쓴다 */
object SyncTestSupport {

    val T0: Instant = Instant.parse("2026-11-28T10:00:00Z")

    fun session(
        clientId: UUID = UUID.randomUUID(),
        startedAt: Instant = T0,
        endedAt: Instant? = null,
        deleted: Boolean = false,
        clientUpdatedAt: Instant = T0,
    ) = SessionChange(
        clientId = clientId,
        startedAt = startedAt,
        endedAt = endedAt,
        deleted = deleted,
        clientUpdatedAt = clientUpdatedAt,
    )

    fun set(
        sessionClientId: UUID,
        exerciseId: Long,
        clientId: UUID = UUID.randomUUID(),
        weightKg: BigDecimal = BigDecimal("60.00"),
        reps: Int = 10,
        rpe: BigDecimal? = null,
        deleted: Boolean = false,
        setOrder: Int = 0,
        clientUpdatedAt: Instant = T0,
    ) = SetChange(
        clientId = clientId,
        sessionClientId = sessionClientId,
        exerciseId = exerciseId,
        exerciseOrder = 0,
        setOrder = setOrder,
        weightKg = weightKg,
        reps = reps,
        rpe = rpe,
        completed = true,
        deleted = deleted,
        performedAt = T0,
        clientUpdatedAt = clientUpdatedAt,
    )

    fun MockMvc.postSync(objectMapper: ObjectMapper, accessToken: String?, body: Any): ResultActionsDsl =
        post("/api/v1/workouts/sync") {
            accessToken?.let { header(HttpHeaders.AUTHORIZATION, "Bearer $it") }
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }

    fun MockMvc.sync(objectMapper: ObjectMapper, accessToken: String, request: SyncRequest): JsonNode =
        objectMapper.readTree(postSync(objectMapper, accessToken, request).andReturn().response.contentAsString)

    /** 결과의 outcome 목록 (요청 순서) */
    fun JsonNode.outcomes(): List<String> = this["results"].map { it["outcome"].asText() }

    /** clientId의 결과 */
    fun JsonNode.resultOf(clientId: UUID): JsonNode = this["results"].first { it["clientId"].asText() == clientId.toString() }
}
