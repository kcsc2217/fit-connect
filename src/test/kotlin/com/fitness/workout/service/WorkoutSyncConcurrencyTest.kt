package com.fitness.workout.service

import com.fitness.support.AuthTestSupport.uniqueDevId
import com.fitness.support.AuthTestSupport.userIdOf
import com.fitness.support.IntegrationTest
import com.fitness.support.SyncTestSupport.session
import com.fitness.support.SyncTestSupport.set
import com.fitness.user.domain.AuthProvider
import com.fitness.user.dto.LoginRequest
import com.fitness.user.security.JwtProvider
import com.fitness.user.service.AuthService
import com.fitness.workout.dto.SyncRequest
import com.fitness.workout.repository.ExerciseRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/** 같은 항목의 동시 동기화 (계획 011). 실제 커밋이 필요해 롤백하지 않는다 */
@IntegrationTest
class WorkoutSyncConcurrencyTest @Autowired constructor(
    private val workoutSyncService: WorkoutSyncService,
    private val authService: AuthService,
    private val jwtProvider: JwtProvider,
    private val exerciseRepository: ExerciseRepository,
    private val jdbcTemplate: JdbcTemplate,
) {

    private val pool = Executors.newFixedThreadPool(THREADS)
    private val bench: Long by lazy { exerciseRepository.findByCode("BENCH_PRESS")!!.id!! }

    @AfterEach
    fun tearDown() {
        pool.shutdownNow()
    }

    private fun newUserId(): Long =
        jwtProvider.userIdOf(authService.login(LoginRequest(AuthProvider.DEV, uniqueDevId())).accessToken)

    private fun setRows(userId: Long): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM workout_set WHERE user_id = ?", Int::class.java, userId)

    private fun <T> submitAll(start: CountDownLatch, tasks: List<() -> T>): List<Future<Result<T>>> =
        tasks.map { task -> pool.submit<Result<T>> { start.await(); runCatching(task) } }

    @Test
    fun `같은 세트를 동시에 10번 보내도 행은 하나이고 모두 성공한다`() {
        // given: 세션을 먼저 올려 두고, 같은 세트를 동시에 보낸다
        val userId = newUserId()
        val s = session()
        workoutSyncService.sync(userId, SyncRequest(sessions = listOf(s)))
        val request = SyncRequest(sets = listOf(set(s.clientId, bench)))
        val start = CountDownLatch(1)
        val futures = submitAll(start, List(THREADS) { { workoutSyncService.sync(userId, request) } })

        // when
        start.countDown()
        val results = futures.map { it.get(20, TimeUnit.SECONDS) }

        // then
        assertThat(results.count { it.isSuccess } to setRows(userId)).isEqualTo(THREADS to 1)
    }

    @Test
    fun `같은 항목 20건을 서로 반대 순서로 담은 묶음 두 개를 동시에 보내도 중복 행 없이 모두 성공한다`() {
        // given: 처리 순서가 정해져 있지 않으면 두 트랜잭션이 서로의 잠금을 기다려 데드락이 날 수 있다
        val userId = newUserId()
        val s = session()
        workoutSyncService.sync(userId, SyncRequest(sessions = listOf(s)))
        val sets = (0 until 20).map { set(s.clientId, bench, setOrder = it) }
        val start = CountDownLatch(1)
        val futures = submitAll(
            start,
            listOf(
                { workoutSyncService.sync(userId, SyncRequest(sets = sets)) },
                { workoutSyncService.sync(userId, SyncRequest(sets = sets.reversed())) },
            ),
        )

        // when
        start.countDown()
        val results = futures.map { it.get(20, TimeUnit.SECONDS) }

        // then
        assertThat(results.count { it.isSuccess } to setRows(userId)).isEqualTo(2 to 20)
    }

    companion object {
        private const val THREADS = 10
    }
}
