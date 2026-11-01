package com.fitness

import com.fitness.support.RepositoryTest
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * 로컬은 ddl-auto=create라 Flyway 마이그레이션이 돌지 않는다.
 * 이 테스트는 마이그레이션으로 만든 스키마에 ddl-auto=validate로 컨텍스트를 띄워,
 * 운영 배포 전에 마이그레이션과 엔티티의 어긋남을 잡는다. (어긋나면 컨텍스트 로딩 단계에서 실패)
 */
@RepositoryTest
class SchemaValidationTest @Autowired constructor(
    private val flyway: Flyway,
) {

    @Test
    fun `Flyway 마이그레이션으로 만든 스키마는 엔티티 매핑과 일치한다`() {
        // given
        val migrations = flyway.info()

        // when
        val pending = migrations.pending()

        // then
        assertThat(pending).isEmpty()
    }
}
