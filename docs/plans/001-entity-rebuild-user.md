# 001 엔티티 재구성: 기반 + user 모듈 구현 계획

- 명세: docs/db-schema.md (3.1 user 모듈, 1장 설계 원칙)
- 크기: L의 일부 (엔티티 재구성 중 1/4)
  - 001 기반 + user · 002 workout · 003 routine·notification·llm · 004 challenge·공통
- 상태: 승인됨 (2026-11-01)

## Context

PRD v2로 서비스가 "PT 클래스 수강신청"에서 "AI 트레이너 앱"으로 바뀌었고, 새 DB 스키마(`docs/db-schema.md`)가 나왔다. 지금 코드의 엔티티 6개(User, Trainer, PtClass, Reservation, DietLog 등)는 새 스키마와 맞는 게 없다.
이 계획은 이후 모듈들이 올라갈 **기반**(Flyway, 시각 UTC 저장, 감사 컬럼, 프로필 분리)을 만들고, 그 기반을 **user 모듈 5개 테이블**로 실제 검증한다.

사용자 결정:
- 로컬은 `ddl-auto: create` 유지, **운영에서만 Flyway**로 테이블을 관리한다.
- 로컬에서 Flyway가 돌지 않으므로, **테스트에서 마이그레이션을 실행하고 엔티티와 비교(validate)** 해서 어긋남을 잡는다. 테스트 DB는 로컬 MySQL의 `fitness_test`.
- spring-kafka는 1차 비목표라 이번에 제거한다.

## 접근

설정을 세 겹으로 둔다. 기본(`application.yml`, 커밋)은 운영 기준인 `Flyway + validate`, 로컬(`application-local.yml`, 미커밋)은 `create + Flyway 끔`, 테스트(`application-test.yml`, 커밋)는 `fitness_test DB + Flyway + validate`. 기본값을 운영 기준으로 두면 프로파일을 깜빡해도 테이블이 날아가지 않는다.
테스트 프로파일은 `local,test` 순서로 켜서 DB 계정은 로컬 설정에서 그대로 가져오고(비밀값을 커밋하지 않음), URL과 스키마 관리 방식만 덮어쓴다.
엔티티는 **모듈 안에서도 연관관계 대신 ID 필드**로 참조한다. FK 제약은 DDL에만 둔다(같은 모듈 안). 지연 로딩·N+1을 원천적으로 피하고, 모듈 간 규칙과 모양을 통일하기 위해서다.

## 변경 파일

| 파일 | 변경 | 내용 |
| --- | --- | --- |
| build.gradle.kts | 수정 | `flyway-core`, `flyway-mysql` 추가 (버전은 Boot 관리, 10.10). `spring-kafka`, `spring-kafka-test` 제거 |
| src/main/resources/application.yml | 수정 | 운영 기준 공통 설정 (아래 스케치) |
| src/main/resources/application-local.yml | 수정 (미커밋 파일) | `ddl-auto: create` 유지, `flyway.enabled: false` 추가, kafka 항목 삭제. 계정 정보는 건드리지 않음 |
| src/test/resources/application-test.yml | 신규 | `fitness_test` URL, validate, Flyway 켬, clean 허용 |
| src/test/kotlin/com/fitness/support/TestFlywayConfig.kt | 신규 | 테스트 컨텍스트 시작 시 `clean → migrate` |
| src/main/resources/db/migration/V1__create_user_tables.sql | 신규 | users, social_account, user_profile, user_equipment, device_token |
| common/entity/BaseTimeEntity.kt | 수정 | `createdAt`·`updatedAt`을 `Instant`로, `@LastModifiedDate` 추가 |
| config/JpaAuditingConfig.kt | 수정 | `Instant.now()`를 주는 `DateTimeProvider` 등록 |
| common/type/Equipment.kt | 신규 | 장비 enum. user_equipment와 (002의) exercise_equipment가 함께 씀 |
| user/domain/* | 교체 | User, UserStatus, SocialAccount, AuthProvider(+APPLE), UserProfile, Sex, Goal, Experience, SplitType, UserEquipment(+UserEquipmentId), DeviceToken, Platform. 옛 UserRole, TargetGoal 삭제 |
| user/repository/* | 교체 | UserRepository, SocialAccountRepository, UserProfileRepository, UserEquipmentRepository, DeviceTokenRepository |
| trainer/, ptclass/, reservation/, diet/ | 삭제 | 옛 도메인 전체 |
| config/SwaggerConfig.kt | 수정 | 제목·설명을 새 서비스로 |
| CLAUDE.md | 수정 | 기술 스택(Flyway, Kafka 제거), 패키지 구조(새 6개 모듈), 도메인 간 참조 규칙(ID 참조, FK는 같은 모듈 DDL에만), 로컬 환경(프로파일 3겹, fitness_test), "전환 중" 안내 축소 |
| docs/decisions/0001-schema-management.md | 신규 | 로컬 create / 운영 Flyway / 테스트 validate |
| docs/decisions/0002-utc-instant.md | 신규 | 시각을 `Instant` + UTC로 저장하는 이유 |

서비스·API는 이번 범위가 아니다 (로그인 E2E에서 만든다).

## 데이터 변경

`docs/db-schema.md` 3.1의 5개 테이블을 그대로 만든다. 모든 테이블에 `created_at`, `updated_at DATETIME(6) NOT NULL`.

| 테이블 | PK | 유니크 / 인덱스 | FK |
| --- | --- | --- | --- |
| users | id | - | - |
| social_account | id | UK `(provider, provider_user_id)`, IDX `(user_id)` | user_id → users |
| user_profile | user_id (직접 지정) | IDX `(alarm_time)` | user_id → users |
| user_equipment | (user_id, equipment) | - | user_id → users |
| device_token | id | UK `(token)`, IDX `(user_id)` | user_id → users |

- 운영: Flyway가 `V1`을 적용. 로컬: Hibernate가 엔티티로 생성(FK·인덱스 이름은 다를 수 있음, 동작에는 영향 없음).
- 로컬 `fitness` DB의 옛 테이블(trainer, pt_class, reservation, diet_log)은 `create`가 지우지 않는다. 남아 있어도 무해하다.

## API

해당 없음 (엔티티·저장소만).

## 핵심 코드 스케치

설정 세 겹:

```yaml
# application.yml — 운영 기준 (커밋)
spring:
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc.time_zone: UTC                          # 읽고 쓸 때 UTC 기준
        type.preferred_instant_jdbc_type: TIMESTAMP  # Instant → DATETIME(6) (기본은 TIMESTAMP 타입)
  flyway:
    enabled: true

# application-local.yml — 로컬 (미커밋, 이 부분만 수정)
spring:
  jpa.hibernate.ddl-auto: create
  flyway.enabled: false

# src/test/resources/application-test.yml — 테스트 (커밋). 계정은 local에서 상속
spring:
  datasource.url: jdbc:mysql://localhost:3306/fitness_test?useSSL=false&characterEncoding=UTF-8
  jpa.hibernate.ddl-auto: validate
  flyway:
    enabled: true
    clean-disabled: false
```

```kotlin
// 테스트 시작 시 매번 빈 DB에서 마이그레이션 → 마이그레이션 파일을 고쳐도 체크섬 오류 없이 다시 검증
@TestConfiguration
class TestFlywayConfig {
    @Bean
    fun cleanMigrateStrategy() = FlywayMigrationStrategy { flyway ->
        flyway.clean()
        flyway.migrate()
    }
}

// 저장소 테스트 공통 설정
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local", "test")
@Import(JpaAuditingConfig::class, TestFlywayConfig::class)
annotation class RepositoryTest
```

```kotlin
// 감사 컬럼: Instant(UTC)
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseTimeEntity {
    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null
        protected set

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: Instant? = null
        protected set
}

// 프로필: users.id를 그대로 PK로 사용 (생성 전략 없음). 연관관계 없이 ID만
@Entity
@Table(name = "user_profile")
class UserProfile(
    @Id val userId: Long,
    @Column(precision = 5, scale = 1) var heightCm: BigDecimal,
    @Column(precision = 5, scale = 1) var weightKg: BigDecimal,
    var birthYear: Short,
    @Enumerated(EnumType.STRING) var goal: Goal,
    var workoutDays: Int,          // 요일 비트마스크, DDL은 TINYINT
    var alarmTime: LocalTime,      // 사용자 시간대 기준
    var timezone: String,
    // ...
) : BaseTimeEntity()

// 복합 키: kotlin-jpa 플러그인이 기본 생성자를 만들어 주도록 @Embeddable 사용 (@IdClass는 안 됨)
@Embeddable
data class UserEquipmentId(val userId: Long, @Enumerated(EnumType.STRING) val equipment: Equipment) : Serializable
```

```sql
-- V1 일부: 중복 가입 방지 유니크와 같은 모듈 FK
CREATE TABLE social_account (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    provider         VARCHAR(20)  NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    email            VARCHAR(255) NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_social_account_provider_user (provider, provider_user_id),
    KEY idx_social_account_user (user_id),
    CONSTRAINT fk_social_account_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
```

**타입 매핑 주의:** `TINYINT`(workout_days)·`SMALLINT`·`DECIMAL`·`TIME`이 Kotlin 타입과 validate에서 어긋날 수 있다. 검증 테스트가 실패하면 `@JdbcTypeCode`나 Kotlin 타입(Short 등)으로 맞춘다. 이 경험을 002(BOOLEAN, BINARY(16) 등)에 그대로 쓴다.

## 정합성·동시성·멱등성

- 중복 가입: `social_account (provider, provider_user_id)` 유니크가 DB에서 막는다. 동시에 두 번 가입 요청이 와도 하나는 제약 위반으로 실패 (서비스에서 처리하는 건 로그인 E2E 범위).
- 같은 FCM 토큰 중복: `device_token (token)` 유니크.
- 프로필·장비는 assigned ID라 `save()`가 merge(SELECT 후 INSERT)로 동작한다. 쿼리 1개가 더 들지만 온보딩 1회성 쓰기라 감수한다.

## 실패·대체 경로

해당 없음 (외부 호출 없음). 단, 테스트와 종료 시 테스트 훅은 **로컬 MySQL이 켜져 있고 `fitness_test` DB가 있어야** 통과한다.

## 테스트 목록

모두 `@RepositoryTest`(실제 MySQL `fitness_test`, 테스트마다 롤백).

- [ ] `Flyway 마이그레이션으로 만든 스키마는 엔티티 매핑과 일치한다` — validate로 컨텍스트가 뜨고 적용 안 된 마이그레이션이 없다
- [ ] `같은 provider와 provider_user_id로 소셜 계정을 두 번 저장하면 유니크 제약 위반 예외가 난다`
- [ ] `같은 FCM 토큰을 두 번 저장하면 유니크 제약 위반 예외가 난다`
- [ ] `존재하지 않는 사용자 id로 프로필을 저장하면 외래 키 위반 예외가 난다` — 마이그레이션에 FK가 실제로 있는지 확인
- [ ] `같은 사용자에게 같은 장비를 두 번 저장해도 한 행만 남는다` — 복합 키 동작
- [ ] `엔티티를 저장하면 createdAt과 updatedAt이 채워진다`
- [ ] `엔티티를 수정하면 updatedAt만 바뀌고 createdAt은 그대로다`
- [ ] `시각은 UTC로 저장된다` — `Instant.parse("2026-11-01T00:00:00Z")`를 저장한 뒤 JdbcTemplate으로 원본 컬럼을 읽어 `2026-11-01 00:00:00`인지 확인 (서버 시간대가 KST여도)

## 작업 순서

1. 사전 준비: 로컬 MySQL에 `fitness_test` DB 생성
2. 의존성: Flyway 추가, Kafka 제거 → 컴파일 확인
3. 설정 세 겹 + `TestFlywayConfig`, `@RepositoryTest`
4. 옛 도메인(trainer, ptclass, reservation, diet)과 옛 user 엔티티 삭제
5. **테스트 작성** → 구현이 없어 실패하는 것 확인 (컴파일용 시그니처만)
6. BaseTimeEntity·Auditing·Equipment → user 엔티티·저장소 → `V1` 마이그레이션
7. 테스트 통과 (타입 매핑 어긋남은 이 단계에서 조정)
8. `./gradlew bootRun`으로 로컬(`create`)에서 앱이 뜨는지 확인
9. CLAUDE.md, SwaggerConfig, ADR 0001·0002
10. `/review-change` → 커밋 (main)

## 검증

- `./gradlew test`: 위 8개 테스트 통과 = 마이그레이션·엔티티 일치, 제약 존재, 시각 UTC 확인
- `./gradlew bootRun`: 로컬 프로파일에서 `create`로 테이블 생성 후 정상 기동, `/actuator/health` UP
- 로컬 `fitness` DB에 user 테이블 5개가 생겼는지 `SHOW TABLES`로 확인 (읽기만)

## 확인 필요

- `fitness_test` DB를 직접 만드실지, 제가 `application-local.yml`의 계정으로 `mysql` 명령을 실행해 만들지 (비밀번호는 출력하지 않음)
  ```sql
  CREATE DATABASE fitness_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```
- `Equipment` enum을 `common/type`에 두는 것 (user·workout 두 모듈이 같은 값을 써서). workout 모듈에 두고 user가 참조하는 방식도 가능
- 로컬 `fitness` DB의 옛 테이블 4개를 지울지 (남겨도 무해, 기본은 그대로 둠)

## 변경 이력

- 2026-11-01: 처음 5개로 나눈 계획 중 001(기반)과 002(user)를 합침. 기반만으로는 검증할 엔티티가 없어서
- 2026-11-01: "확인 필요" 3개는 답이 없어 기본값으로 진행. fitness_test DB는 Claude가 로컬 계정으로 생성, Equipment는 common/type, 로컬 fitness DB의 옛 테이블은 그대로 둠
- 2026-11-01 (리뷰 반영): `hibernate.jdbc.time_zone=UTC`가 LocalTime까지 서버 시간대만큼 밀어 저장(18:30 → 09:30)해서, DB 연결 시간대 UTC 고정으로 바꿈(ADR 0002 개정). 테스트 JVM을 Asia/Seoul로 고정. `application-local.yml`을 jar에서 제외. 테스트 clean 전에 DB 이름 `_test` 확인. 요일 비트마스크 0~127 검증. 장비 PK·createdAt 유지 테스트를 DB까지 확인하도록 변경. "로컬은 FK 이름이 다를 수 있음"은 사실과 달라 정정: 로컬 create에서는 FK가 생기지 않음
