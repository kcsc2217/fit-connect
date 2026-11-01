package com.fitness.common.entity

import com.fitness.common.type.Equipment
import com.fitness.support.RepositoryTest
import com.fitness.support.UserFixtures
import com.fitness.user.domain.UserEquipment
import com.fitness.user.domain.UserEquipmentId
import com.fitness.user.repository.UserEquipmentRepository
import com.fitness.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.time.temporal.ChronoUnit

/** 감사 컬럼(created_at, updated_at)과 시각 UTC 저장을 users 테이블로 확인한다. */
@RepositoryTest
class BaseTimeEntityTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val userEquipmentRepository: UserEquipmentRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val entityManager: TestEntityManager,
) {

    @Test
    fun `엔티티를 저장하면 createdAt과 updatedAt이 채워진다`() {
        // given
        val user = UserFixtures.user()

        // when
        val saved = userRepository.saveAndFlush(user)

        // then
        assertThat(listOf(saved.createdAt, saved.updatedAt)).doesNotContainNull()
    }

    @Test
    fun `엔티티를 수정하면 DB의 updatedAt만 바뀌고 createdAt은 그대로다`() {
        // given
        val user = userRepository.saveAndFlush(UserFixtures.user())
        val createdAt = user.createdAt!!.truncatedTo(ChronoUnit.MICROS) // DATETIME(6) 정밀도
        val updatedAt = user.updatedAt!!.truncatedTo(ChronoUnit.MICROS)
        Thread.sleep(5) // 감사 시각이 확실히 달라지도록

        // when
        user.nickname = "바뀐닉네임"
        userRepository.saveAndFlush(user)

        // then
        entityManager.clear() // 메모리 값이 아니라 DB 값을 다시 읽는다
        val reloaded = userRepository.findById(user.id!!).get()
        assertThat(reloaded.updatedAt).isAfter(updatedAt)
        assertThat(reloaded.createdAt).isEqualTo(createdAt)
    }

    @Test
    fun `createdAt이 비어 있는 객체로 다시 저장해도 DB의 createdAt은 처음 값으로 유지된다`() {
        // given: 복합 키처럼 ID를 직접 정하는 엔티티는 새 객체를 save하면 merge된다.
        // 새 객체의 createdAt은 null이라, updatable = false가 없으면 DB 값을 null로 덮으려 한다.
        val userId = userRepository.saveAndFlush(UserFixtures.user()).id!!
        val id = UserEquipmentId(userId, Equipment.BARBELL)
        val createdAt = userEquipmentRepository.saveAndFlush(UserEquipment(id)).createdAt!!.truncatedTo(ChronoUnit.MICROS)
        entityManager.clear()

        // when
        userEquipmentRepository.saveAndFlush(UserEquipment(id))

        // then
        entityManager.clear()
        assertThat(userEquipmentRepository.findById(id).get().createdAt).isEqualTo(createdAt)
    }

    @Test
    fun `시각은 UTC로 저장된다`() {
        // given
        val user = userRepository.saveAndFlush(UserFixtures.user())
        user.withdraw(Instant.parse("2026-11-01T00:00:00Z"))
        userRepository.saveAndFlush(user)

        // when
        val stored = jdbcTemplate.queryForObject(
            "SELECT DATE_FORMAT(withdrawn_at, '%Y-%m-%d %H:%i:%s') FROM users WHERE id = ?",
            String::class.java,
            user.id,
        )

        // then
        assertThat(stored).isEqualTo("2026-11-01 00:00:00")
    }
}
