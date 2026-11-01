package com.fitness.user.repository

import com.fitness.support.RepositoryTest
import com.fitness.support.UserFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalTime

/**
 * 알림 시각은 "사용자가 사는 곳 기준 18:30" 같은 벽시계 값이라 시간대 변환 없이 저장돼야 한다.
 * JPA로 다시 읽으면 변환이 되돌려져 티가 안 나므로, DB 원본 값을 직접 읽어 확인한다.
 */
@RepositoryTest
class UserProfileStorageTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val jdbcTemplate: JdbcTemplate,
) {

    @Test
    fun `알림 시각은 시간대 변환 없이 그대로 저장된다`() {
        // given
        val userId = userRepository.saveAndFlush(UserFixtures.user()).id!!
        val profile = UserFixtures.profile(userId).apply { alarmTime = LocalTime.of(18, 30) }

        // when
        userProfileRepository.saveAndFlush(profile)

        // then
        val stored = jdbcTemplate.queryForObject(
            "SELECT TIME_FORMAT(alarm_time, '%H:%i:%s') FROM user_profile WHERE user_id = ?",
            String::class.java,
            userId,
        )
        assertThat(stored).isEqualTo("18:30:00")
    }
}
