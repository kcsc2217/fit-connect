package com.fitness.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fitness.support.AuthTestSupport.loginAsDev
import com.fitness.support.AuthTestSupport.userIdOf
import com.fitness.support.IntegrationTest
import com.fitness.support.ProfileTestSupport.putProfile
import com.fitness.support.ProfileTestSupport.validProfileJson
import com.fitness.support.UserFixtures
import com.fitness.user.repository.UserRepository
import com.fitness.user.security.JwtProvider
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** 온보딩 프로필 API (계획 009). 롤백하지 않으므로 테스트마다 새 개발용 회원으로 로그인한다 */
@IntegrationTest
class UserProfileControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val jwtProvider: JwtProvider,
    private val userRepository: UserRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val clock: Clock,
) {

    private fun newAccess(): String = mockMvc.loginAsDev(objectMapper).accessToken

    private fun getMe(access: String) = mockMvc.get("/api/v1/users/me") {
        header(HttpHeaders.AUTHORIZATION, "Bearer $access")
    }

    private fun getProfile(access: String) = mockMvc.get("/api/v1/users/me/profile") {
        header(HttpHeaders.AUTHORIZATION, "Bearer $access")
    }

    @Test
    fun `온보딩 회원이 프로필을 제출하면 상태가 ACTIVE인 프로필을 돌려준다`() {
        // given
        val access = newAccess()

        // when
        val result = mockMvc.putProfile(objectMapper, access, validProfileJson())

        // then
        result.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("ACTIVE") }
            jsonPath("$.goal") { value("BULK") }
        }
    }

    @Test
    fun `프로필을 제출하면 내 정보 조회의 상태도 ACTIVE가 된다`() {
        // given
        val access = newAccess()
        mockMvc.putProfile(objectMapper, access, validProfileJson())

        // when
        val result = getMe(access)

        // then
        result.andExpect { jsonPath("$.status") { value("ACTIVE") } }
    }

    @Test
    fun `제출한 프로필을 조회하면 요일은 월요일부터, 알림 시각은 시-분으로 돌려준다`() {
        // given
        val access = newAccess()
        mockMvc.putProfile(
            objectMapper,
            access,
            validProfileJson("workoutDays" to listOf("FRIDAY", "MONDAY"), "alarmTime" to "07:05"),
        )

        // when
        val result = getProfile(access)

        // then
        result.andExpect {
            status { isOk() }
            jsonPath("$.workoutDays") { value(contains("MONDAY", "FRIDAY")) }
            jsonPath("$.alarmTime") { value("07:05") }
        }
    }

    @Test
    fun `같은 프로필을 두 번 제출해도 같은 결과를 돌려준다`() {
        // given
        val access = newAccess()
        // 첫 제출이 성공해야 비교가 의미 있다 (둘 다 같은 오류여도 본문은 같으므로)
        val first = mockMvc.putProfile(objectMapper, access, validProfileJson())
            .andExpect { status { isOk() } }
            .andReturn().response.contentAsString

        // when
        val second = mockMvc.putProfile(objectMapper, access, validProfileJson()).andReturn().response.contentAsString

        // then
        assertThat(objectMapper.readTree(second)).isEqualTo(objectMapper.readTree(first))
    }

    @Test
    fun `ACTIVE 회원이 다시 제출하면 빠진 장비는 지우고 새 장비는 추가한다`() {
        // given
        val access = newAccess()
        mockMvc.putProfile(objectMapper, access, validProfileJson("equipment" to listOf("BARBELL", "DUMBBELL")))
            .andExpect { status { isOk() } } // 첫 제출이 저장돼야 "빠진 장비 삭제" 경로를 탄다

        // when
        mockMvc.putProfile(objectMapper, access, validProfileJson("equipment" to listOf("DUMBBELL", "CABLE")))

        // then
        getProfile(access).andExpect { jsonPath("$.equipment") { value(contains("DUMBBELL", "CABLE")) } }
    }

    @Test
    fun `장비를 빈 목록으로 제출하면 장비가 모두 지워진다`() {
        // given
        val access = newAccess()
        mockMvc.putProfile(objectMapper, access, validProfileJson("equipment" to listOf("BARBELL")))
            .andExpect { status { isOk() } }

        // when
        mockMvc.putProfile(objectMapper, access, validProfileJson("equipment" to emptyList<String>()))

        // then
        getProfile(access).andExpect { jsonPath("$.equipment") { isEmpty() } }
    }

    @Test
    fun `운동 요일이 비어 있으면 400 INVALID_INPUT과 workoutDays 필드 오류로 응답한다`() {
        // given
        val body = validProfileJson("workoutDays" to emptyList<String>())

        // when
        val result = mockMvc.putProfile(objectMapper, newAccess(), body)

        // then
        result.andExpectInvalidField("workoutDays")
    }

    @Test
    fun `키가 250cm를 넘으면 400 INVALID_INPUT과 heightCm 필드 오류로 응답한다`() {
        // given
        val body = validProfileJson("heightCm" to 250.1)

        // when
        val result = mockMvc.putProfile(objectMapper, newAccess(), body)

        // then
        result.andExpectInvalidField("heightCm")
    }

    @Test
    fun `몸무게가 소수 둘째 자리까지 있으면 400 INVALID_INPUT으로 응답한다`() {
        // given
        val body = validProfileJson("weightKg" to 72.55)

        // when
        val result = mockMvc.putProfile(objectMapper, newAccess(), body)

        // then
        result.andExpectInvalidField("weightKg")
    }

    @Test
    fun `나이가 14세 미만인 출생 연도면 400 INVALID_INPUT과 birthYear 필드 오류로 응답한다`() {
        // given: 서울 기준 올해 13세가 되는 출생 연도
        val thisYear = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul"))).year
        val body = validProfileJson("birthYear" to thisYear - 13)

        // when
        val result = mockMvc.putProfile(objectMapper, newAccess(), body)

        // then
        result.andExpectInvalidField("birthYear")
    }

    @Test
    fun `알림 시각이 5분 단위가 아니면 400 INVALID_INPUT과 alarmTime 필드 오류로 응답한다`() {
        // given
        val body = validProfileJson("alarmTime" to "19:03")

        // when
        val result = mockMvc.putProfile(objectMapper, newAccess(), body)

        // then
        result.andExpectInvalidField("alarmTime")
    }

    @Test
    fun `운동 시간이 선택지에 없으면 400 INVALID_INPUT과 sessionMinutes 필드 오류로 응답한다`() {
        // given
        val body = validProfileJson("sessionMinutes" to 50)

        // when
        val result = mockMvc.putProfile(objectMapper, newAccess(), body)

        // then
        result.andExpectInvalidField("sessionMinutes")
    }

    @Test
    fun `시간대 이름이 잘못되면 400 INVALID_INPUT과 timezone 필드 오류로 응답한다`() {
        // given
        val body = validProfileJson("timezone" to "Asia/Nowhere")

        // when
        val result = mockMvc.putProfile(objectMapper, newAccess(), body)

        // then
        result.andExpectInvalidField("timezone")
    }

    @Test
    fun `모르는 목표 값을 보내면 400 INVALID_INPUT과 goal 필드 오류로 응답한다`() {
        // given
        val body = validProfileJson("goal" to "FLY")

        // when
        val result = mockMvc.putProfile(objectMapper, newAccess(), body)

        // then
        result.andExpectInvalidField("goal")
    }

    @Test
    fun `검증에 실패하면 회원 상태는 ONBOARDING 그대로다`() {
        // given
        val access = newAccess()
        mockMvc.putProfile(objectMapper, access, validProfileJson("sessionMinutes" to 50))
            .andExpect { status { isBadRequest() } } // 인증 실패 등 다른 이유가 아니라 검증 실패여야 한다

        // when
        val result = getMe(access)

        // then
        result.andExpect { jsonPath("$.status") { value("ONBOARDING") } }
    }

    @Test
    fun `토큰 없이 제출하면 401 UNAUTHORIZED로 응답한다`() {
        // given
        val body = validProfileJson()

        // when
        val result = mockMvc.putProfile(objectMapper, null, body)

        // then
        result.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("UNAUTHORIZED") }
        }
    }

    @Test
    fun `탈퇴한 회원이 제출하면 403 FORBIDDEN으로 응답한다`() {
        // given
        val user = UserFixtures.user().apply { withdraw(clock.instant()) }
        val userId = userRepository.save(user).id!!
        val access = jwtProvider.issueAccessToken(userId).value

        // when
        val result = mockMvc.putProfile(objectMapper, access, validProfileJson())

        // then
        result.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }
    }

    @Test
    fun `토큰은 유효한데 회원이 없으면 제출은 404 NOT_FOUND로 응답한다`() {
        // given
        val access = jwtProvider.issueAccessToken(Long.MAX_VALUE).value

        // when
        val result = mockMvc.putProfile(objectMapper, access, validProfileJson())

        // then
        result.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("NOT_FOUND") }
        }
    }

    @Test
    fun `온보딩 전에 프로필을 조회하면 404 NOT_FOUND로 응답한다`() {
        // given
        val access = newAccess()

        // when
        val result = getProfile(access)

        // then
        result.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("NOT_FOUND") }
        }
    }

    @Test
    fun `운동 요일은 비트마스크로 저장된다`() {
        // given: 월(1) + 수(4) + 금(16) = 21
        val access = newAccess()
        val userId = jwtProvider.userIdOf(access)

        // when
        mockMvc.putProfile(objectMapper, access, validProfileJson("workoutDays" to listOf("MONDAY", "WEDNESDAY", "FRIDAY")))

        // then: JPA로 다시 읽으면 변환이 숨으므로 DB 원본 값을 읽는다
        val raw = jdbcTemplate.queryForObject("select workout_days from user_profile where user_id = ?", Int::class.java, userId)
        assertThat(raw).isEqualTo(21)
    }

    @Test
    fun `운동 요일 목록에 빈 값이 있으면 400 INVALID_INPUT과 workoutDays 필드 오류로 응답한다`() {
        // given
        val body = validProfileJson("workoutDays" to listOf("MONDAY", null))

        // when
        val result = mockMvc.putProfile(objectMapper, newAccess(), body)

        // then
        result.andExpectInvalidField("workoutDays")
    }

    @Test
    fun `장비 목록에 빈 값이 있으면 400 INVALID_INPUT과 equipment 필드 오류로 응답한다`() {
        // given
        val body = validProfileJson("equipment" to listOf("BARBELL", null))

        // when
        val result = mockMvc.putProfile(objectMapper, newAccess(), body)

        // then
        result.andExpectInvalidField("equipment")
    }

    @Test
    fun `출생 연도가 숫자 범위를 넘으면 400 INVALID_INPUT과 birthYear 필드 오류로 응답한다`() {
        // given: Short 범위(32767) 밖
        val body = validProfileJson("birthYear" to 70000)

        // when
        val result = mockMvc.putProfile(objectMapper, newAccess(), body)

        // then
        result.andExpectInvalidField("birthYear")
    }

    @Test
    fun `운동 시간을 소수로 보내면 잘라서 받지 않고 400 INVALID_INPUT으로 응답한다`() {
        // given
        val body = validProfileJson("sessionMinutes" to 60.9)

        // when
        val result = mockMvc.putProfile(objectMapper, newAccess(), body)

        // then
        result.andExpectInvalidField("sessionMinutes")
    }

    @Test
    fun `올해 14세가 되는 출생 연도는 받는다`() {
        // given: 하한 경계
        val body = validProfileJson("birthYear" to seoulThisYear() - 14)

        // when
        val result = mockMvc.putProfile(objectMapper, newAccess(), body)

        // then
        result.andExpect { status { isOk() } }
    }

    @Test
    fun `올해 100세가 되는 출생 연도는 받는다`() {
        // given: 상한 경계
        val body = validProfileJson("birthYear" to seoulThisYear() - 100)

        // when
        val result = mockMvc.putProfile(objectMapper, newAccess(), body)

        // then
        result.andExpect { status { isOk() } }
    }

    @Test
    fun `올해 101세가 되는 출생 연도면 400 INVALID_INPUT과 birthYear 필드 오류로 응답한다`() {
        // given: 상한 바로 밖
        val body = validProfileJson("birthYear" to seoulThisYear() - 101)

        // when
        val result = mockMvc.putProfile(objectMapper, newAccess(), body)

        // then
        result.andExpectInvalidField("birthYear")
    }

    @Test
    fun `키가 정확히 250cm면 받는다`() {
        // given: 상한 경계
        val body = validProfileJson("heightCm" to 250.0)

        // when
        val result = mockMvc.putProfile(objectMapper, newAccess(), body)

        // then
        result.andExpect { status { isOk() } }
    }

    // 요청의 시간대(validProfileJson 기본값 Asia/Seoul) 기준 올해
    private fun seoulThisYear(): Int = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul"))).year

    private fun org.springframework.test.web.servlet.ResultActionsDsl.andExpectInvalidField(field: String) =
        andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_INPUT") }
            jsonPath("$.errors[0].field") { value(field) }
        }
}
