package com.fitness.workout.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fitness.support.AuthTestSupport.loginAsDev
import com.fitness.support.AuthTestSupport.userIdOf
import com.fitness.support.IntegrationTest
import com.fitness.support.SyncTestSupport.T0
import com.fitness.support.SyncTestSupport.outcomes
import com.fitness.support.SyncTestSupport.postSync
import com.fitness.support.SyncTestSupport.resultOf
import com.fitness.support.SyncTestSupport.session
import com.fitness.support.SyncTestSupport.set
import com.fitness.support.SyncTestSupport.sync
import com.fitness.support.UserFixtures
import com.fitness.support.WorkoutFixtures
import com.fitness.workout.service.WorkoutSyncService
import org.redisson.api.RedissonClient
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.fitness.user.repository.UserRepository
import com.fitness.user.security.JwtProvider
import com.fitness.workout.dto.SyncRequest
import com.fitness.workout.repository.ExerciseRepository
import com.fitness.workout.repository.WorkoutSessionRepository
import com.fitness.workout.repository.WorkoutSetRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.util.UUID

/** 운동 기록 묶음 동기화 (계획 011). 롤백하지 않으므로 테스트마다 새 개발용 회원과 새 UUID를 쓴다 */
@IntegrationTest
class WorkoutSyncControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val jwtProvider: JwtProvider,
    private val userRepository: UserRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val workoutSetRepository: WorkoutSetRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val clock: Clock,
    private val redissonClient: RedissonClient,
) {

    private val bench: Long by lazy { exerciseRepository.findByCode("BENCH_PRESS")!!.id!! }

    private fun newAccess(): String = mockMvc.loginAsDev(objectMapper).accessToken

    private fun setCount(userId: Long, clientId: UUID): Int = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM workout_set WHERE user_id = ? AND client_id = UUID_TO_BIN(?)",
        Int::class.java,
        userId,
        clientId.toString(),
    )

    @Test
    fun `처음 보낸 세션과 세트를 저장하고 모두 APPLIED로 응답한다`() {
        // given
        val s = session()
        val request = SyncRequest(listOf(s), listOf(set(s.clientId, bench), set(s.clientId, bench, setOrder = 1)))

        // when
        val response = mockMvc.sync(objectMapper, newAccess(), request)

        // then
        assertThat(response.outcomes()).containsExactly("APPLIED", "APPLIED", "APPLIED")
    }

    @Test
    fun `같은 묶음을 다시 보내면 행이 늘지 않고 IGNORED로 응답한다`() {
        // given
        val access = newAccess()
        val s = session()
        val t = set(s.clientId, bench)
        val request = SyncRequest(listOf(s), listOf(t))
        mockMvc.sync(objectMapper, access, request)

        // when
        val response = mockMvc.sync(objectMapper, access, request)

        // then
        assertThat(response.outcomes() to setCount(jwtProvider.userIdOf(access), t.clientId))
            .isEqualTo(listOf("IGNORED", "IGNORED") to 1)
    }

    @Test
    fun `더 새로운 수정 시각의 세트는 새 값으로 덮어쓴다`() {
        // given
        val access = newAccess()
        val s = session()
        val t = set(s.clientId, bench, weightKg = BigDecimal("60.00"))
        mockMvc.sync(objectMapper, access, SyncRequest(listOf(s), listOf(t)))

        // when
        mockMvc.sync(
            objectMapper,
            access,
            SyncRequest(sets = listOf(t.copy(weightKg = BigDecimal("62.50"), clientUpdatedAt = T0.plusSeconds(60)))),
        )

        // then
        val saved = workoutSetRepository.findByUserIdAndClientId(jwtProvider.userIdOf(access), t.clientId)!!
        assertThat(saved.weightKg).isEqualByComparingTo("62.50")
    }

    @Test
    fun `더 오래된 수정 시각의 세트는 무시하고 기존 값을 유지한다`() {
        // given
        val access = newAccess()
        val s = session()
        val t = set(s.clientId, bench, weightKg = BigDecimal("60.00"), clientUpdatedAt = T0.plusSeconds(60))
        mockMvc.sync(objectMapper, access, SyncRequest(listOf(s), listOf(t)))

        // when
        val response = mockMvc.sync(
            objectMapper,
            access,
            SyncRequest(sets = listOf(t.copy(weightKg = BigDecimal("50.00"), clientUpdatedAt = T0))),
        )

        // then
        val saved = workoutSetRepository.findByUserIdAndClientId(jwtProvider.userIdOf(access), t.clientId)!!
        assertThat(response.outcomes() to saved.weightKg.toPlainString()).isEqualTo(listOf("IGNORED") to "60.00")
    }

    @Test
    fun `삭제한 세트는 지우지 않고 deleted로 남긴다`() {
        // given
        val access = newAccess()
        val s = session()
        val t = set(s.clientId, bench)
        mockMvc.sync(objectMapper, access, SyncRequest(listOf(s), listOf(t)))

        // when
        mockMvc.sync(
            objectMapper,
            access,
            SyncRequest(sets = listOf(t.copy(deleted = true, clientUpdatedAt = T0.plusSeconds(60)))),
        )

        // then
        val saved = workoutSetRepository.findByUserIdAndClientId(jwtProvider.userIdOf(access), t.clientId)
        assertThat(saved?.deleted).isTrue()
    }

    @Test
    fun `삭제된 세트에 더 새로운 수정이 와도 되살리지 않는다`() {
        // given
        val access = newAccess()
        val s = session()
        val t = set(s.clientId, bench)
        mockMvc.sync(objectMapper, access, SyncRequest(listOf(s), listOf(t.copy(deleted = true))))

        // when
        val response = mockMvc.sync(
            objectMapper,
            access,
            SyncRequest(sets = listOf(t.copy(deleted = false, clientUpdatedAt = T0.plusSeconds(600)))),
        )

        // then
        val saved = workoutSetRepository.findByUserIdAndClientId(jwtProvider.userIdOf(access), t.clientId)!!
        assertThat(response.outcomes() to saved.deleted).isEqualTo(listOf("IGNORED") to true)
    }

    @Test
    fun `세션 종료 시각을 나중에 보내면 같은 세션에 채워진다`() {
        // given
        val access = newAccess()
        val s = session()
        mockMvc.sync(objectMapper, access, SyncRequest(sessions = listOf(s)))
        val endedAt = T0.plus(Duration.ofMinutes(58))

        // when
        mockMvc.sync(
            objectMapper,
            access,
            SyncRequest(sessions = listOf(s.copy(endedAt = endedAt, clientUpdatedAt = endedAt))),
        )

        // then
        val saved = workoutSessionRepository.findByUserIdAndClientId(jwtProvider.userIdOf(access), s.clientId)!!
        assertThat(saved.endedAt).isEqualTo(endedAt)
    }

    @Test
    fun `세션이 없는 세트는 SESSION_NOT_FOUND로 실패하고 나머지 항목은 저장한다`() {
        // given
        val s = session()
        val ok = set(s.clientId, bench)
        val orphan = set(UUID.randomUUID(), bench)

        // when
        val response = mockMvc.sync(objectMapper, newAccess(), SyncRequest(listOf(s), listOf(ok, orphan)))

        // then
        assertThat(response.resultOf(orphan.clientId)["reason"].asText() to response.resultOf(ok.clientId)["outcome"].asText())
            .isEqualTo("SESSION_NOT_FOUND" to "APPLIED")
    }

    @Test
    fun `다른 사용자의 세션을 가리키는 세트는 SESSION_NOT_FOUND로 실패한다`() {
        // given: 다른 회원이 만든 세션
        val othersSession = session()
        mockMvc.sync(objectMapper, newAccess(), SyncRequest(sessions = listOf(othersSession)))
        val t = set(othersSession.clientId, bench)

        // when
        val response = mockMvc.sync(objectMapper, newAccess(), SyncRequest(sets = listOf(t)))

        // then
        assertThat(response.resultOf(t.clientId)["reason"].asText()).isEqualTo("SESSION_NOT_FOUND")
    }

    @Test
    fun `같은 client_id라도 사용자가 다르면 따로 저장된다`() {
        // given: 두 회원이 같은 UUID로 세션을 만든다
        val clientId = UUID.randomUUID()
        mockMvc.sync(objectMapper, newAccess(), SyncRequest(sessions = listOf(session(clientId = clientId))))

        // when
        val response = mockMvc.sync(objectMapper, newAccess(), SyncRequest(sessions = listOf(session(clientId = clientId))))

        // then: 둘째 회원의 것도 새로 저장된다 (남의 행을 갱신하지 않음)
        val rows = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM workout_session WHERE client_id = UUID_TO_BIN(?)",
            Int::class.java,
            clientId.toString(),
        )
        assertThat(response.outcomes() to rows).isEqualTo(listOf("APPLIED") to 2)
    }

    @Test
    fun `모르는 종목의 세트는 UNKNOWN_EXERCISE로 실패한다`() {
        // given
        val s = session()
        val t = set(s.clientId, exerciseId = Long.MAX_VALUE)

        // when
        val response = mockMvc.sync(objectMapper, newAccess(), SyncRequest(listOf(s), listOf(t)))

        // then
        assertThat(response.resultOf(t.clientId)["reason"].asText()).isEqualTo("UNKNOWN_EXERCISE")
    }

    @Test
    fun `반복 수가 0이면 INVALID_VALUE로 실패한다`() {
        // given
        val s = session()
        val t = set(s.clientId, bench, reps = 0)

        // when
        val response = mockMvc.sync(objectMapper, newAccess(), SyncRequest(listOf(s), listOf(t)))

        // then
        assertThat(response.resultOf(t.clientId)["reason"].asText()).isEqualTo("INVALID_VALUE")
    }

    @Test
    fun `RPE가 0점5 단위가 아니면 INVALID_VALUE로 실패한다`() {
        // given
        val s = session()
        val t = set(s.clientId, bench, rpe = BigDecimal("8.3"))

        // when
        val response = mockMvc.sync(objectMapper, newAccess(), SyncRequest(listOf(s), listOf(t)))

        // then
        assertThat(response.resultOf(t.clientId)["reason"].asText()).isEqualTo("INVALID_VALUE")
    }

    @Test
    fun `종료가 시작보다 빠른 세션은 INVALID_VALUE로 실패한다`() {
        // given
        val s = session(endedAt = T0.minusSeconds(1))

        // when
        val response = mockMvc.sync(objectMapper, newAccess(), SyncRequest(sessions = listOf(s)))

        // then
        assertThat(response.resultOf(s.clientId)["reason"].asText()).isEqualTo("INVALID_VALUE")
    }

    @Test
    fun `묶음이 100건을 넘으면 400 INVALID_INPUT으로 응답하고 아무것도 저장하지 않는다`() {
        // given: 세션 1건 + 세트 100건
        val access = newAccess()
        val s = session()
        val request = SyncRequest(listOf(s), (0 until 100).map { set(s.clientId, bench, setOrder = it) })

        // when
        val result = mockMvc.postSync(objectMapper, access, request)

        // then
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_INPUT") }
        }
        assertThat(workoutSessionRepository.findByUserIdAndClientId(jwtProvider.userIdOf(access), s.clientId)).isNull()
    }

    @Test
    fun `빈 묶음이면 400 INVALID_INPUT으로 응답한다`() {
        // given
        val request = SyncRequest()

        // when
        val result = mockMvc.postSync(objectMapper, newAccess(), request)

        // then
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_INPUT") }
        }
    }

    @Test
    fun `수정 시각이 서버 시각보다 5분 넘게 미래면 서버 수신 시각으로 낮춰 저장한다`() {
        // given: 기기 시계가 하루 앞서 있다
        val access = newAccess()
        val before = clock.instant()
        val s = session(clientUpdatedAt = before.plus(Duration.ofDays(1)))

        // when
        mockMvc.sync(objectMapper, access, SyncRequest(sessions = listOf(s)))

        // then
        val saved = workoutSessionRepository.findByUserIdAndClientId(jwtProvider.userIdOf(access), s.clientId)!!
        assertThat(saved.clientUpdatedAt).isBetween(before, clock.instant())
    }

    @Test
    fun `결과는 요청 순서대로 돌려준다`() {
        // given: client_id 정렬 순서와 요청 순서가 반대가 되도록 내림차순 고정 UUID (랜덤이면 우연히 같을 수 있다)
        val s = session()
        val sets = (5 downTo 1).map {
            set(s.clientId, bench, clientId = UUID.fromString("00000000-0000-0000-0000-00000000000$it"), setOrder = it)
        }
        val request = SyncRequest(listOf(s), sets)

        // when
        val response = mockMvc.sync(objectMapper, newAccess(), request)

        // then
        val expected = listOf(s.clientId) + sets.map { it.clientId }
        assertThat(response["results"].map { UUID.fromString(it["clientId"].asText()) }).isEqualTo(expected)
    }

    @Test
    fun `세션 시작 시각은 UTC 그대로 저장된다`() {
        // given: JPA로 다시 읽으면 변환이 되돌려져 버그가 안 보이므로 DB 원본 값을 읽는다
        val access = newAccess()
        val s = session(startedAt = T0)

        // when
        mockMvc.sync(objectMapper, access, SyncRequest(sessions = listOf(s)))

        // then
        val raw = jdbcTemplate.queryForObject(
            "SELECT DATE_FORMAT(started_at, '%Y-%m-%d %H:%i:%s') FROM workout_session " +
                "WHERE user_id = ? AND client_id = UUID_TO_BIN(?)",
            String::class.java,
            jwtProvider.userIdOf(access),
            s.clientId.toString(),
        )
        assertThat(raw).isEqualTo("2026-11-28 10:00:00")
    }

    @Test
    fun `JDBC로 저장한 client_id를 JPA로 읽으면 같은 UUID다`() {
        // given
        val access = newAccess()
        val s = session(clientId = UUID.fromString("0192f5a1-7c3e-7d4b-9a2f-3e5d6c7b8a91"))

        // when
        mockMvc.sync(objectMapper, access, SyncRequest(sessions = listOf(s)))

        // then
        val saved = workoutSessionRepository.findByUserIdAndClientId(jwtProvider.userIdOf(access), s.clientId)
        assertThat(saved?.clientId).isEqualTo(s.clientId)
    }

    @Test
    fun `토큰 없이 보내면 401 UNAUTHORIZED로 응답한다`() {
        // given
        val request = SyncRequest(sessions = listOf(session()))

        // when
        val result = mockMvc.postSync(objectMapper, null, request)

        // then
        result.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("UNAUTHORIZED") }
        }
    }

    @Test
    fun `탈퇴한 회원이 보내면 403 FORBIDDEN으로 응답한다`() {
        // given
        val user = UserFixtures.user().apply { withdraw(clock.instant()) }
        val access = jwtProvider.issueAccessToken(userRepository.save(user).id!!).value

        // when
        val result = mockMvc.postSync(objectMapper, access, SyncRequest(sessions = listOf(session())))

        // then
        result.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }
    }
    @Test
    fun `시각이 저장할 수 있는 범위를 벗어난 세션은 INVALID_VALUE로 실패하고 나머지 항목은 저장한다`() {
        // given: DB DATETIME 범위(9999년)를 넘는 시작 시각 + 정상 세션
        val bad = session(startedAt = Instant.parse("+10000-01-01T00:00:00Z"))
        val ok = session()

        // when
        val response = mockMvc.sync(objectMapper, newAccess(), SyncRequest(sessions = listOf(bad, ok)))

        // then: 묶음 전체가 500이 되지 않는다
        assertThat(response.resultOf(bad.clientId)["reason"].asText() to response.resultOf(ok.clientId)["outcome"].asText())
            .isEqualTo("INVALID_VALUE" to "APPLIED")
    }

    @Test
    fun `2000년보다 이른 수행 시각의 세트는 INVALID_VALUE로 실패한다`() {
        // given
        val s = session()
        val t = set(s.clientId, bench).copy(performedAt = Instant.parse("1999-12-31T23:59:59Z"))

        // when
        val response = mockMvc.sync(objectMapper, newAccess(), SyncRequest(listOf(s), listOf(t)))

        // then
        assertThat(response.resultOf(t.clientId)["reason"].asText()).isEqualTo("INVALID_VALUE")
    }

    @Test
    fun `목록에 빈 원소가 있으면 400 INVALID_INPUT으로 응답한다`() {
        // given: Jackson은 목록의 null 원소를 그대로 넣는다
        val body = mapOf("sessions" to listOf(null))

        // when
        val result = mockMvc.postSync(objectMapper, newAccess(), body)

        // then
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_INPUT") }
        }
    }

    @Test
    fun `한 묶음에 같은 client_id가 두 번 있으면 400 INVALID_INPUT으로 응답한다`() {
        // given
        val s = session()
        val request = SyncRequest(sessions = listOf(s, s.copy(clientUpdatedAt = T0.plusSeconds(1))))

        // when
        val result = mockMvc.postSync(objectMapper, newAccess(), request)

        // then
        result.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_INPUT") }
        }
    }

    @Test
    fun `저장 뒤 종목이 비활성화돼도 그 세트를 지울 수 있다`() {
        // given: 테스트 전용 종목으로 세트를 저장한 뒤 종목을 비활성화한다
        val access = newAccess()
        val exercise = exerciseRepository.save(WorkoutFixtures.exercise("TEST_SYNC_" + UUID.randomUUID().toString().take(8)))
        val s = session()
        val t = set(s.clientId, exercise.id!!)
        mockMvc.sync(objectMapper, access, SyncRequest(listOf(s), listOf(t)))
        exercise.active = false
        exerciseRepository.save(exercise)

        // when
        mockMvc.sync(objectMapper, access, SyncRequest(sets = listOf(t.copy(deleted = true, clientUpdatedAt = T0.plusSeconds(60)))))

        // then
        val saved = workoutSetRepository.findByUserIdAndClientId(jwtProvider.userIdOf(access), t.clientId)
        assertThat(saved?.deleted).isTrue()
    }

    @Test
    fun `같은 회원의 다른 동기화가 락을 오래 잡고 있으면 503 SYNC_BUSY로 응답한다`() {
        // given: 다른 스레드가 이 회원의 동기화 락을 잡고 있다 (Redisson 락은 같은 스레드면 다시 잡을 수 있어서)
        val access = newAccess()
        val held = CountDownLatch(1)
        val release = CountDownLatch(1)
        val holder = Executors.newSingleThreadExecutor()
        holder.submit {
            val lock = redissonClient.getLock(WorkoutSyncService.lockKey(jwtProvider.userIdOf(access)))
            lock.lock()
            held.countDown()
            release.await()
            lock.unlock()
        }
        held.await(10, TimeUnit.SECONDS)

        // when
        val result = mockMvc.postSync(objectMapper, access, SyncRequest(sessions = listOf(session())))

        // then
        release.countDown()
        holder.shutdown()
        result.andExpect {
            status { isServiceUnavailable() }
            jsonPath("$.code") { value("SYNC_BUSY") }
        }
    }
}
