package com.fitness.user.service

import com.fitness.support.AuthTestSupport.uniqueDevId
import com.fitness.support.AuthTestSupport.userIdOf
import com.fitness.support.IntegrationTest
import com.fitness.user.domain.AuthProvider
import com.fitness.user.dto.LoginRequest
import com.fitness.user.repository.UserRepository
import com.fitness.user.security.JwtProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/** 여러 스레드가 실제로 커밋해야 하므로 롤백하지 않는다. 테스트마다 고유한 소셜 ID를 쓴다 */
@IntegrationTest
class AuthServiceConcurrencyTest @Autowired constructor(
    private val authService: AuthService,
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider,
) {

    private val pool = Executors.newFixedThreadPool(THREADS)

    @AfterEach
    fun tearDown() {
        pool.shutdownNow()
    }

    @Test
    fun `같은 refresh로 동시에 갱신하면 하나만 성공한다`() {
        // given
        val refresh = authService.login(LoginRequest(AuthProvider.DEV, uniqueDevId())).refreshToken
        val start = CountDownLatch(1)
        val futures = submitAll(start) { authService.refresh(refresh) }

        // when
        start.countDown()
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }

        // then
        assertThat(results.count { it.isSuccess }).isEqualTo(1)
    }

    @Test
    fun `같은 소셜 계정으로 동시에 처음 로그인하면 모두 같은 회원으로 토큰을 받는다`() {
        // given
        val request = LoginRequest(AuthProvider.DEV, uniqueDevId())
        val start = CountDownLatch(1)
        val futures = submitAll(start) { authService.login(request) }

        // when
        start.countDown()
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }

        // then
        val userIds = results.map { jwtProvider.userIdOf(it.getOrThrow().accessToken) }
        assertThat(userIds.distinct()).hasSize(1)
    }

    @Test
    fun `같은 소셜 계정으로 동시에 처음 로그인해도 회원 행은 하나만 늘어난다`() {
        // given
        val request = LoginRequest(AuthProvider.DEV, uniqueDevId())
        val before = userRepository.count()
        val start = CountDownLatch(1)
        val futures = submitAll(start) { authService.login(request) }

        // when
        start.countDown()
        futures.forEach { it.get(10, TimeUnit.SECONDS) }

        // then
        assertThat(userRepository.count() - before).isEqualTo(1)
    }

    // 모든 스레드가 시작 신호를 기다렸다가 한꺼번에 action을 실행한다. 예외는 Result에 담는다
    private fun <T> submitAll(start: CountDownLatch, action: () -> T): List<Future<Result<T>>> =
        (1..THREADS).map {
            pool.submit<Result<T>> {
                start.await()
                runCatching(action)
            }
        }

    companion object {
        private const val THREADS = 5
    }
}
