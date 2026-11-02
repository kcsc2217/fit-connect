package com.fitness.common

import com.fitness.support.RepositoryTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

/** shedlock은 엔티티가 없어 validate가 검사하지 않으므로, ShedLock이 요구하는 형식을 직접 확인한다. */
@RepositoryTest
class ShedlockTableTest @Autowired constructor(
    private val jdbcTemplate: JdbcTemplate,
) {

    @Test
    fun `shedlock 테이블은 ShedLock 형식대로 만들어진다`() {
        // given
        val sql = """
            SELECT CONCAT(column_name, ' ', column_type, ' ', is_nullable, ' ', column_key)
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'shedlock'
            ORDER BY ordinal_position
        """.trimIndent()

        // when
        val columns = jdbcTemplate.queryForList(sql, String::class.java).map { it.trim() }

        // then
        assertThat(columns).containsExactly(
            "name varchar(64) NO PRI",
            "lock_until timestamp(3) NO",
            "locked_at timestamp(3) NO",
            "locked_by varchar(255) NO",
        )
    }
}
