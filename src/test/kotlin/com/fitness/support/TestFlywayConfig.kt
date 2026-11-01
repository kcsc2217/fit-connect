package com.fitness.support

import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

/**
 * 테스트 컨텍스트가 뜰 때마다 빈 DB에서 마이그레이션을 다시 적용한다.
 * 개발 중 마이그레이션 파일을 고쳐도 체크섬 오류 없이 최신 파일로 검증하기 위해서다.
 */
@TestConfiguration
class TestFlywayConfig {

    @Bean
    fun cleanMigrateStrategy() = FlywayMigrationStrategy { flyway ->
        requireTestDatabase(flyway)
        flyway.clean()
        flyway.migrate()
    }

    // clean은 모든 테이블을 지운다. 환경변수(SPRING_DATASOURCE_URL 등)가 테스트 설정을 덮어써
    // 다른 DB에 연결된 경우를 막기 위해, DB 이름이 _test로 끝날 때만 진행한다.
    private fun requireTestDatabase(flyway: Flyway) {
        val database = flyway.configuration.dataSource.connection.use { it.catalog }
        check(database.endsWith("_test")) {
            "테스트가 '$database' DB에 연결돼 있어 clean을 중단합니다. 테스트 DB 이름은 _test로 끝나야 합니다."
        }
    }
}
