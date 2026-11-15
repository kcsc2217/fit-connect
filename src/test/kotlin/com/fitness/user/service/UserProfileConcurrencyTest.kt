package com.fitness.user.service

import com.fitness.common.type.Equipment
import com.fitness.support.AuthTestSupport.uniqueDevId
import com.fitness.support.AuthTestSupport.userIdOf
import com.fitness.support.IntegrationTest
import com.fitness.support.ProfileTestSupport.validProfileRequest
import com.fitness.user.domain.AuthProvider
import com.fitness.user.dto.LoginRequest
import com.fitness.user.repository.UserEquipmentRepository
import com.fitness.user.security.JwtProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** 같은 회원의 온보딩 동시 제출 (계획 009). 실제 커밋이 필요해 롤백하지 않는다 */
@IntegrationTest
class UserProfileConcurrencyTest @Autowired constructor(
    private val userProfileService: UserProfileService,
    private val authService: AuthService,
    private val jwtProvider: JwtProvider,
    private val userEquipmentRepository: UserEquipmentRepository,
    private val jdbcTemplate: JdbcTemplate,
) {

    private val pool = Executors.newFixedThreadPool(THREADS)

    @AfterEach
    fun tearDown() {
        pool.shutdownNow()
    }

    private fun newUserId(): Long {
        val access = authService.login(LoginRequest(AuthProvider.DEV, uniqueDevId())).accessToken
        return jwtProvider.userIdOf(access)
    }

    @Test
    fun `같은 회원이 처음 제출을 동시에 10번 보내도 모두 성공하고 프로필은 하나다`() {
        // given
        val userId = newUserId()
        val start = CountDownLatch(1)
        val futures = (1..THREADS).map {
            pool.submit<Result<Unit>> {
                start.await()
                runCatching { userProfileService.submit(userId, validProfileRequest()) }.map { }
            }
        }

        // when
        start.countDown()
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }

        // then
        val profiles = jdbcTemplate.queryForObject("select count(*) from user_profile where user_id = ?", Int::class.java, userId)
        assertThat(results.count { it.isSuccess } to profiles).isEqualTo(THREADS to 1)
    }

    @Test
    fun `같은 회원이 장비가 다른 제출을 동시에 보내도 장비는 어느 한 요청의 목록과 같다`() {
        // given
        val userId = newUserId()
        val listA = listOf(Equipment.BARBELL, Equipment.RACK)
        val listB = listOf(Equipment.DUMBBELL, Equipment.CABLE, Equipment.BENCH)
        val start = CountDownLatch(1)
        val futures = (1..THREADS).map { i ->
            pool.submit {
                start.await()
                userProfileService.submit(userId, validProfileRequest(if (i % 2 == 0) listA else listB))
            }
        }

        // when
        start.countDown()
        futures.forEach { it.get(10, TimeUnit.SECONDS) }

        // then
        val saved = userEquipmentRepository.findAllByIdUserId(userId).map { it.id.equipment }.toSet()
        assertThat(saved).isIn(listA.toSet(), listB.toSet())
    }

    companion object {
        private const val THREADS = 10
    }
}
