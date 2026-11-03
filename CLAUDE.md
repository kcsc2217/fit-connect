# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Traffic-Gym (나만의 트레이너 쌤)

내 신체 정보와 운동 기록을 아는 AI 트레이너가 매일 오늘의 운동·식단을 짜서 알려주는 앱의 백엔드. 요구사항은 `docs/prd.md`(v2), DB는 `docs/db-schema.md`가 기준이다.

> **진행 상황:** 엔티티 재구성(`docs/plans/001`~`004`)으로 모든 모듈(user, workout, routine, notification, llm, challenge, analytics)의 엔티티와 마이그레이션(V1~V5)을 만들었다. 다음은 기능(API) 구현이다.

## 아키텍처 원칙
- 숫자(무게·세트·반복 수·칼로리)는 서버 규칙이 계산한다. LLM은 종목 구성·순서·문구만 만든다.
- LLM에는 서버가 SQL로 거른 후보 종목만 넘긴다. LLM 응답은 JSON 스키마·규칙 검증을 통과해야 저장하고, 실패하면 대체 응답(기본 루틴)을 쓴다.
- 멱등성이 필요한 쓰기는 `(user_id, client_id)` 유니크 제약 + upsert로 처리한다.
- 동시성이 필요한 쓰기(챌린지 정원 등)는 `docs/decisions/`의 결정을 따른다. Redis 원자 연산 + DB 유니크 제약을 최후 안전망으로 둔다.
- 서버 메모리에 상태를 두지 않는다 (여러 대로 늘려도 동작해야 한다).
- 성공 응답은 DTO를 감싸지 않고 그대로 반환한다. 에러는 ProblemDetail + `code`로 응답한다 (`docs/decisions/0003-error-response-format.md`). 우리 코드는 `BusinessException(ErrorCode)`를 던지고, 새 에러 코드는 기능을 만들 때 `ErrorCode`에 추가한다.

## 작업 절차
모든 작업은 `docs/workflow.md`의 절차(명세 → 계획 → 테스트 먼저 → 구현 → 리뷰 → 성능 확인 → 기록)를 따른다. 각 단계에는 `.claude/skills/`의 스킬이 있다.

- **작업을 시작할 때 크기(S/M/L)를 제안하고 사용자 확정을 받는다.** 기준은 위험도다. 동시성·멱등성·LLM이 걸리면 최소 M.
- S: 채팅으로 계획(고칠 파일, 내용, 테스트)을 말하고 승인받은 뒤 테스트 → 구현.
- M/L: `/plan-feature`로 계획 모드에서 계획(핵심 코드 스케치 포함)을 쓰고, 승인되면 `docs/plans/`에 저장한 뒤 테스트부터 작성한다. L은 계획을 M 단위로 쪼갠다.

## 하지 말 것
- **계획(`docs/plans/`)이 사용자 승인을 받기 전에 구현을 시작하지 않는다.**
- 엔티티를 API 응답으로 직접 반환하지 않는다. 반드시 DTO로 변환한다.
- 테스트 없이 작업을 완료 처리하지 않는다. 모든 API는 통합 테스트를 가진다.
- LLM 응답을 검증 없이 저장하거나 사용자에게 보여주지 않는다.
- 비밀값(API 키, DB 비밀번호)을 코드나 커밋되는 파일에 넣지 않는다.

## 기술 스택
- Kotlin 1.9.25 / JDK 21 / Spring Boot 3.3.3 / Gradle Kotlin DSL
- MySQL 8.x (JPA + QueryDSL, Flyway), Redis (Spring Data Redis + Redisson)
- Actuator + Micrometer(Prometheus), springdoc-openapi(Swagger)
- 테스트: JUnit5, Mockk, AssertJ
- 카프카는 1차 비목표라 쓰지 않는다 (측정으로 필요가 확인될 때 도입).

## 개발 명령어

```bash
./gradlew build                 # 전체 빌드 (컴파일 + 테스트)
./gradlew bootRun               # 로컬 실행 (local 프로파일, 8080 포트)
./gradlew test                  # 전체 테스트 (로컬 MySQL 필요, 아래 "DB 스키마와 엔티티" 참고)
./gradlew test --tests "com.fitness.user.repository.UserRepositoryConstraintTest"              # 클래스 단위
./gradlew test --tests "com.fitness.user.repository.UserRepositoryConstraintTest.같은 FCM*"     # 메서드 단위 (백틱 테스트명은 글롭 패턴으로 매칭)
```

- Gradle 래퍼는 **8.10 고정**이다. Kotlin 1.9.25의 kapt가 Gradle 9의 API 변경과 호환되지 않아 임의로 올리면 빌드가 깨진다.
- QueryDSL Q클래스는 kapt가 `build/generated/source/kapt/main`에 생성한다. 엔티티 필드를 바꾼 뒤 Q클래스가 안 잡히면 `./gradlew clean compileKotlin`으로 재생성한다.
- formatter는 프로젝트에 없고, ktlint가 로컬에 설치돼 있으면 훅이 수정한 파일에만 적용한다.

### 자동 검증 훅 (`.claude/settings.json`)
- Kotlin 파일 수정 직후: ktlint 포맷(설치 시) + `compileKotlin compileTestKotlin`. 실패하면 에러가 돌아오니 바로 고친다.
- 작업 종료 시: 코드 변경이 있으면 `./gradlew test`. 실패하면 최대 3번까지 스스로 고친다.
- 훅이 잡은 오류는 `.claude/hooks/caught.log`에 쌓인다 (`docs/ai-log.md` 기록용).
- 턴 시작·끝 시각은 `.claude/hooks/timing.log`에 쌓인다 (UserPromptSubmit·Stop 훅). ai-log의 "Claude 작업 시간" 계산용.

## 패키지 구조

모듈러 모놀리스. **모듈(도메인)별 패키지, 모듈 내부는 레이어별**로 구성한다. (`com.fitness.<module>.<layer>`) 모듈 목록은 PRD 10장, 테이블 소유는 `docs/db-schema.md` 1장을 따른다.

```
com.fitness
├── FitnessApplication.kt
├── config/              # 전역 설정 빈 (JPA Auditing, Swagger, Redis 등)
├── common/              # 모듈에 속하지 않는 공통 코드
│   ├── entity/          # BaseTimeEntity
│   ├── type/            # 여러 모듈이 같이 쓰는 값 (Equipment 등)
│   └── exception/       # GlobalExceptionHandler, BusinessException, ErrorCode (에러 응답 형식은 ADR 0003)
├── user/                # 소셜 로그인, JWT, 온보딩 프로필, 장비, 기기 토큰   ← 구현됨
├── workout/             # 종목 사전, 세션·세트 기록, 오프라인 동기화, PR·주간 볼륨   ← 엔티티·후보 종목 조회 구현됨
├── routine/             # 오늘의 운동 생성 (서버 규칙 + LLM), 기본 루틴 대체   ← 엔티티 구현됨
├── notification/        # 알림 예약, FCM 푸시, 발송 기록   ← 엔티티 구현됨
├── llm/                 # LLM 클라이언트, 프롬프트 버전, 호출 로그, 재시도·대체   ← 엔티티 구현됨
├── challenge/           # 챌린지 선착순 신청 (동시성 실험)   ← 엔티티 구현됨
└── analytics/           # 제품 지표 이벤트 기록 (event_log)   ← 엔티티 구현됨
```

각 모듈 안: `controller/`, `service/`, `domain/`(엔티티·enum), `repository/`, `dto/`. 외부 API 클라이언트는 `client/`.

### 모듈 간 참조 규칙
- 다른 모듈의 **Repository를 직접 주입받지 않는다.** 그 모듈의 Service(공개 인터페이스)를 통해서만 접근한다.
- 엔티티는 **연관관계(`@ManyToOne` 등) 대신 ID 필드**(`userId: Long`)로 참조한다. 모듈 안에서도 마찬가지다.
- 외래 키 제약은 **같은 모듈 안에서만** 마이그레이션 DDL에 건다. 모듈 간에는 ID + 인덱스만 둔다.
- `common`, `config`는 모듈 코드에 의존하지 않는다 (역방향 의존 금지).

## DB 스키마와 엔티티
- 스키마 관리 방식은 `docs/decisions/0001-schema-management.md`. 운영은 Flyway(`src/main/resources/db/migration/V*.sql`) + `ddl-auto=validate`, 로컬은 `ddl-auto=create`.
- **테이블을 바꿀 때는 마이그레이션과 엔티티를 함께 고친다.** 운영에 적용된 마이그레이션 파일은 고치지 않고 새 버전을 추가한다.
- 엔티티에도 유니크 제약·인덱스를 `@Table`에 적는다 (로컬 `create`가 같은 제약을 만들도록).
- 모든 엔티티는 `common.entity.BaseTimeEntity`(`createdAt`, `updatedAt`)를 상속한다.
- 시각은 `Instant`(UTC, `DATETIME(6)`), 사용자 기준 날짜는 `LocalDate`, 사용자 기준 시각은 `LocalTime` (`docs/decisions/0002-utc-instant.md`).
  - UTC 고정은 DB 연결 시간대(`connectionTimeZone=UTC`)로 한다. `hibernate.jdbc.time_zone`은 `LocalTime`까지 밀어서 저장하므로 쓰지 않고, JDBC URL에 `serverTimezone`도 넣지 않는다.
  - 시간 값 컬럼을 추가하면 DB 원본 값을 직접 읽는 테스트를 함께 쓴다 (JPA로 다시 읽으면 변환이 되돌려져 버그가 안 보인다).
- 상태값은 `@Enumerated(EnumType.STRING)` + `VARCHAR`. 무게·키·체중은 `BigDecimal` + `precision/scale`.
- DDL 타입이 Kotlin 타입과 다르게 매핑되면(`TINYINT` 등) `@JdbcTypeCode`로 맞춘다. 맞는지는 `SchemaValidationTest`가 확인한다.
- 복합 키는 `@IdClass`가 아니라 `@Embeddable` + `@EmbeddedId` (kotlin-jpa 플러그인이 기본 생성자를 만들어 주도록).
  - ID를 직접 정하는 엔티티를 새 객체로 `save()`하면 merge되고, 반환된 객체의 `createdAt`은 null이다. 응답에 쓸 때는 다시 조회한다.
- 로컬 `create`에서는 FK가 생기지 않는다 (엔티티에 연관관계가 없어서). FK 위반은 저장소 테스트로 확인한다.
- `shedlock`은 ShedLock 라이브러리가 쓰는 테이블이라 엔티티가 없다. 그래서 로컬 `create`에서는 생기지 않는다 (ShedLock 도입 시 로컬 생성 방법을 정한다). 형식은 `ShedlockTableTest`가 확인한다.
- 앱이 만든 UUID(`client_id`)는 Kotlin `UUID` → `BINARY(16)`. 별도 설정 없이 매핑된다.
- 초기 종목 사전은 `docs/data/*.csv`(free-exercise-db에서 고른 종목)가 원본이다. 바꿀 때는 CSV를 고치고 `python3 scripts/generate_exercise_seed.py`로 `V3__seed_exercises.sql`을 다시 만든다 (운영 적용 후에는 새 버전으로 추가). 로컬은 `application-local.yml`의 `spring.sql.init`이 같은 V3 파일을 실행한다. 운영 배포 후 종목을 V4 이후로 추가하면 `spring.sql.init.data-locations`에도 그 파일을 추가해야 로컬에 반영된다. `ExerciseSeedTest`가 DB 내용을 CSV와 통째로 비교한다.
- 맨몸 종목은 `exercise_equipment` 행이 없다 (장비와 관계없이 항상 후보). 종목 조회는 다른 모듈에서 `ExerciseService`를 통해서만 한다.

## 테스트 작성 규칙

모든 테스트는 **Given-When-Then**을 지켜서 작성한다.

- 테스트 메서드 본문을 `// given`, `// when`, `// then` 주석으로 3구간 구분한다. 각 구간은 빈 줄로 분리한다.
- `given`: 테스트 대상과 협력 객체 준비, Mockk 스텁 설정.
- `when`: 검증하려는 동작 단 한 번 호출. 여러 동작을 한 when에 넣지 않는다.
- `then`: 결과 검증(assertion) 및 필요한 경우 `verify`(Mockk) 호출.
- 테스트 함수명은 백틱을 사용한 한글 문장으로, "어떤 상황에서 무엇을 한다"가 드러나게 짓는다.
  예: `` fun `같은 FCM 토큰을 두 번 저장하면 유니크 제약 위반 예외가 난다`() ``
- 하나의 테스트는 하나의 시나리오만 검증한다 (then에서 서로 다른 관심사를 여러 개 assert하지 않는다).
- Mock은 Mockk(`mockk()`, `every`, `verify`)를 사용하고, `spring-boot-starter-test`의 Mockito는 쓰지 않는다.
- 저장소·제약 테스트는 `@RepositoryTest`(`src/test/kotlin/com/fitness/support/`)를 붙인다. 로컬 MySQL의 `fitness_test`에 Flyway를 적용해 실제 제약으로 검증하고, 테스트마다 롤백된다. 픽스처는 `support/UserFixtures` 같은 객체에 둔다.
- API 테스트는 `@IntegrationTest`(`support/`)를 붙인다. 앱 전체를 띄워 MockMvc(Kotlin DSL `mockMvc.get {}`)로 요청하고, DB 설정은 `@RepositoryTest`와 같다.

```kotlin
@Test
fun `같은 FCM 토큰을 두 번 저장하면 유니크 제약 위반 예외가 난다`() {
    // given
    val userId = userRepository.saveAndFlush(UserFixtures.user()).id!!
    deviceTokenRepository.saveAndFlush(DeviceToken(userId, "fcm-token-1", Platform.ANDROID, now))

    // when & then
    assertThrows<DataIntegrityViolationException> {
        deviceTokenRepository.saveAndFlush(DeviceToken(userId, "fcm-token-1", Platform.IOS, now))
    }
}
```

## Git 커밋 규칙
- 커밋 메시지는 **한글로 작성**한다.

## 로컬 실행 환경
- 인프라: MySQL, Redis — 둘 다 로컬에 직접 설치되어 있음 (Docker 미사용). 접속 정보(계정/비밀번호)는 `application-local.yml`(gitignore 대상, 커밋 금지)에 각자 설정.
- Redis는 `brew services`가 이 macOS에서 프로세스를 못 띄우는 이슈가 있어 `redis-server /opt/homebrew/etc/redis.conf --daemonize yes`로 수동 기동한다.
- 설정은 세 겹이다.
  - `application.yml`(커밋): 운영 기준. Flyway 켬, `ddl-auto=validate`, 시각 UTC. 기본 활성 프로파일은 `local`.
  - `application-local.yml`(미커밋, jar에서 제외): `fitness` DB 접속 정보, `ddl-auto=create`, Flyway 끔, 초기 종목 시드 실행(`spring.jpa.defer-datasource-initialization: true`, `spring.sql.init.mode: always`, `spring.sql.init.encoding: UTF-8`, `spring.sql.init.data-locations: classpath:db/migration/V3__seed_exercises.sql`).
  - `src/test/resources/application-test.yml`(커밋): `fitness_test` DB(없으면 자동 생성), Flyway + validate. 테스트는 `local,test` 프로파일로 떠서 계정은 local 설정을 그대로 쓴다. DB 이름이 `_test`로 끝나지 않으면 clean 전에 멈춘다.
  - 테스트 JVM은 항상 `Asia/Seoul` 시간대로 돈다 (UTC 장비에서 시간대 버그가 숨지 않게).
  - **local에 추가한 설정은 테스트에도 상속된다.** 테스트에 맞지 않는 설정(시드 실행 등)은 `application-test.yml`에서 꺼야 한다. 그래서 테스트는 `spring.sql.init.mode: never`, `defer-datasource-initialization: false`로 Flyway만 쓴다.
- 테스트와 종료 시 테스트 훅은 로컬 MySQL이 켜져 있어야 통과한다. `@IntegrationTest`는 Redisson이 시작할 때 Redis에 연결하므로 로컬 Redis도 켜져 있어야 한다.
- 로컬·테스트 JDBC URL에는 `useSSL=false&allowPublicKeyRetrieval=true`를 둔다. 없으면 MySQL 비밀번호를 바꾼 직후 `Public Key Retrieval is not allowed`로 접속이 실패하고, 앱에서는 `Unable to determine Dialect without JDBC metadata`로 보인다.
- `gradle.properties`가 아니라 `~/.gradle/gradle.properties`(전역, 미커밋)의 `org.gradle.java.home`이 Homebrew JDK 21을 가리킨다 — 로컬 JDK가 25뿐이라 Gradle 8.10 데몬 구동용으로 별도 지정한 것.
