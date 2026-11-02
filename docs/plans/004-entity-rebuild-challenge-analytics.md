# 004 엔티티 재구성: challenge 모듈·공통(analytics, shedlock) 구현 계획

- 명세: docs/db-schema.md (3.6 challenge, 3.7 공통, 1장 설계 원칙)
- 크기: M — L의 일부 (엔티티 재구성 중 4/4, 마지막)
  - 001 기반 + user ✅ · 002 workout ✅ · 003 routine·notification·llm ✅ · **004 challenge·공통**
- 상태: 승인됨 (2026-11-02)

## Context

엔티티 재구성의 마지막 단계다. 남은 4개 테이블을 만든다. 하나는 선착순 정원과 중복 신청을 막는 동시성 실험의 기반인 `challenge`, `challenge_participation`이다. 나머지 둘은 제품 지표용 `event_log`와 스케줄러 중복 실행을 막는 `shedlock`이다.
신청 API와 4가지 동시성 방식 비교(비관적 락, 낙관적 락, Redis 원자 연산, 대기열)는 이번 범위가 아니다. 그 실험이 기대는 **중복 신청 유니크**와 **낙관적 락 버전 컬럼**을 정확히 만들고 검증한다.

사용자 결정:
- `event_log`는 **analytics 모듈을 새로 만들어** 둔다 (`com.fitness.analytics`). 제품 지표는 기능이라 모듈로 둔다. `common`은 기반 코드(타입, 예외, BaseTimeEntity)만 유지한다.

## 접근

003과 같은 방식으로 `V5__create_challenge_analytics_shedlock_tables.sql` + 엔티티 + 저장소 + 저장소 테스트를 만든다. 정해진 규칙(ID 참조, 같은 모듈 FK만, `Instant`/`LocalDate`, enum은 `VARCHAR` + `EnumType.STRING`, `BaseTimeEntity` 상속)을 따른다.
새로 등장하는 것은 세 가지다.
1. **`@Version`:** `challenge.version`, `challenge_participation.version`은 JPA `@Version var version: Int`로 둔다. 낙관적 락 실험이 이 컬럼을 그대로 쓴다.
2. **JSON 컬럼:** `event_log.properties`는 `Map<String, Any?>` + `@JdbcTypeCode(SqlTypes.JSON)`로 매핑한다. Jackson(spring-boot-starter-web, jackson-module-kotlin)이 이미 있어 추가 의존성이 없다.
3. **엔티티 없는 테이블:** `shedlock`은 ShedLock 라이브러리가 직접 읽고 쓰는 테이블이라 엔티티를 만들지 않고 V5에만 넣는다 (validate는 엔티티 없는 테이블을 검사하지 않음). ShedLock 의존성과 설정은 스케줄러를 만들 때 추가한다.

## 변경 파일

| 파일 | 변경 | 내용 |
| --- | --- | --- |
| db/migration/V5__create_challenge_analytics_shedlock_tables.sql | 신규 | 4개 테이블, 유니크·인덱스, challenge_participation → challenge FK |
| challenge/domain/Challenge.kt, ChallengeStatus.kt | 신규 | 챌린지 (`@Version`, 기간·정원 검증) |
| challenge/domain/ChallengeParticipation.kt, ParticipationStatus.kt | 신규 | 신청 (`@Version`) |
| challenge/repository/ChallengeRepository.kt, ChallengeParticipationRepository.kt | 신규 | `JpaRepository` |
| analytics/domain/EventLog.kt, EventName.kt | 신규 | 이벤트 기록 (JSON 속성) |
| analytics/repository/EventLogRepository.kt | 신규 | `JpaRepository` |
| test/.../support/ChallengeFixtures.kt | 신규 | 챌린지·신청·이벤트 픽스처 |
| test/.../challenge/domain/ChallengeTest.kt | 신규 | 기간·정원 검증 (단위) |
| test/.../challenge/repository/ChallengeConstraintTest.kt, ChallengeStorageTest.kt | 신규 | 유니크·FK·낙관적 락, 시각 저장 |
| test/.../analytics/repository/EventLogStorageTest.kt | 신규 | JSON 저장 |
| test/.../common/ShedlockTableTest.kt | 신규 | shedlock 형식 |
| docs/db-schema.md | 수정 | 1장 모듈별 표에 analytics 추가 (event_log 이동), 3.7 정리 |
| CLAUDE.md | 수정 | 패키지 구조에 analytics 추가, 진행 상황을 "엔티티 재구성 완료"로 갱신 |

enum 값:
- `ChallengeStatus`: SCHEDULED, OPEN, IN_PROGRESS, FINISHED
- `ParticipationStatus`: HOLD, APPLIED, WAITING, CANCELED, EXPIRED
- `EventName`: APP_OPEN, PUSH_OPEN, ROUTINE_VIEW, SESSION_COMPLETE (명세의 예시 4개, 나머지는 기능을 만들 때 추가)

## 데이터 변경

`docs/db-schema.md` 3.6~3.7을 그대로 만든다. 엔티티가 있는 테이블에는 `created_at`, `updated_at`을 둔다 (shedlock은 라이브러리 형식이라 제외).

| 테이블 | PK | 유니크 / 인덱스 | FK (같은 모듈만) |
| --- | --- | --- | --- |
| challenge | id | - | - |
| challenge_participation | id | **UK `(challenge_id, user_id)`**, IDX `(challenge_id, status, wait_seq)`, `(status, hold_expires_at)` | challenge_id → challenge |
| event_log | id | IDX `(name, occurred_at)`, `(user_id, occurred_at)` | - |
| shedlock | name | - | - |

## API

해당 없음 (엔티티·저장소만).

## 핵심 코드 스케치

```sql
-- 최후의 중복 신청 안전망. 취소 후 재신청은 새 행이 아니라 상태 변경이라 유니크를 깨지 않는다
CREATE TABLE challenge_participation
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    challenge_id    BIGINT      NOT NULL,
    user_id         BIGINT      NOT NULL COMMENT '모듈 간 참조, FK 없음',
    status          VARCHAR(20) NOT NULL COMMENT 'HOLD, APPLIED, WAITING, CANCELED, EXPIRED',
    wait_seq        INT         NULL COMMENT '대기 순번',
    hold_expires_at DATETIME(6) NULL COMMENT '임시 확보 만료 시각',
    applied_at      DATETIME(6) NULL COMMENT '확정 시각',
    version         INT         NOT NULL COMMENT '낙관적 락',
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_challenge_participation_challenge_user (challenge_id, user_id),
    KEY idx_challenge_participation_waiting (challenge_id, status, wait_seq),
    KEY idx_challenge_participation_hold_expires (status, hold_expires_at),
    CONSTRAINT fk_challenge_participation_challenge FOREIGN KEY (challenge_id) REFERENCES challenge (id)
) ...;

-- ShedLock(JdbcTemplateLockProvider) 기본 형식 그대로
CREATE TABLE shedlock
(
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at  TIMESTAMP(3) NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
```

```kotlin
// 낙관적 락 실험이 그대로 쓰는 버전. JPA가 UPDATE ... WHERE version = ?로 갱신하고 0건이면 예외
@Version
@Column(nullable = false)
var version: Int = 0
    protected set

// 기간이 뒤집히거나 정원이 0 이하인 챌린지는 만들지 않는다
init {
    require(!endsOn.isBefore(startsOn)) { "종료일은 시작일보다 앞설 수 없습니다: $startsOn ~ $endsOn" }
    require(capacity > 0) { "정원은 1 이상이어야 합니다: $capacity" }
}

// 이벤트 속성: 이벤트마다 키가 달라 JSON으로 둔다
@JdbcTypeCode(SqlTypes.JSON)
@Column(columnDefinition = "json")
val properties: Map<String, Any?>? = null
```

## 정합성·동시성·멱등성

- **중복 신청:** `(challenge_id, user_id)` 유니크가 최후 안전망이다. 4가지 동시성 방식 중 무엇을 쓰든 같은 사용자의 두 번째 행은 DB가 막는다. 취소 후 재신청은 상태 변경으로 처리한다 (신청 API에서).
- **정원 초과:** `applied_count`·`version`은 이번에 컬럼만 만든다. 비관적 락(`FOR UPDATE`), 낙관적 락(`@Version`), Redis Lua, 대기열 중 어떤 방식으로 막을지는 신청 API 계획에서 `docs/decisions/`에 ADR로 정한다.
- **낙관적 락 동작:** 다른 곳에서 먼저 갱신된 챌린지를 저장하면 예외가 나는지 테스트로 확인한다 (실험의 전제).
- **스케줄러 중복 실행:** shedlock 테이블만 만든다. 로컬은 `ddl-auto=create`라 엔티티 없는 이 테이블이 생기지 않는다. ShedLock을 도입할 때 로컬 생성 방법을 함께 정한다 (확인 필요 참고).

## 실패·대체 경로

해당 없음 (외부 호출 없음).

## 테스트 목록

저장소 테스트는 모두 `@RepositoryTest`. `SchemaValidationTest`는 V5가 추가되면 새 테이블까지 자동 검증한다 (JSON·`@Version` 매핑 포함).

challenge
- [ ] `같은 사용자가 같은 챌린지에 신청 행을 두 번 만들면 유니크 제약 위반 예외가 난다`
- [ ] `다른 사용자는 같은 챌린지에 신청할 수 있다` — 유니크 범위가 (챌린지, 사용자)인지
- [ ] `존재하지 않는 챌린지 id로 신청하면 외래 키 위반 예외가 난다`
- [ ] `다른 곳에서 먼저 수정된 챌린지를 저장하면 낙관적 락 예외가 난다` — 읽은 뒤 JDBC로 version을 올리고, 읽어 둔 엔티티를 고쳐 저장
- [ ] `일반 신청 오픈 시각은 UTC로 저장된다` — `2026-11-01T23:00:00Z` → DB 원본 `2026-11-01 23:00:00.000000`
- [ ] `종료일이 시작일보다 앞서면 IllegalArgumentException을 던진다` (단위)

analytics
- [ ] `이벤트 속성은 JSON으로 저장된다` — `{"routineId": 5}` 저장 후 DB에서 `JSON_EXTRACT(properties, '$.routineId')` = 5

공통
- [ ] `shedlock 테이블은 ShedLock 형식대로 만들어진다` — information_schema로 컬럼 4개의 이름·타입, PK가 name인지 확인

## 작업 순서

1. enum·엔티티·저장소 뼈대 (컴파일)
2. **테스트 작성** → V5가 없어 실패 확인 (`missing table`)
3. `V5__create_challenge_analytics_shedlock_tables.sql` → 테스트 통과 (JSON 매핑 어긋남은 이 단계에서 조정)
4. 검증 테스트가 잡는지 확인: 유니크 제약을 빼면 해당 테스트가 실패하는지
5. `./gradlew bootRun`으로 로컬 기동 확인
6. `db-schema.md`, CLAUDE.md 갱신 → `/review-change` → 커밋 (main) → ai-log 기록

## 검증

- `./gradlew test`: 기존 41개 + 새 테스트 8개 통과
- `./gradlew bootRun`: 로컬 정상 기동, `/actuator/health` UP (이번에는 제가 띄운 프로세스만 종료)

## 확인 필요

- `waitlist_capacity`(정원의 50%)는 엔티티에서 계산하지 않고 값을 받는다. 50% 규칙은 챌린지 생성 기능에서 정한다.
- 로컬 shedlock 테이블: ShedLock 도입 시 `application-local.yml`의 `sql.init`에 shedlock DDL을 추가하는 방식을 제안할 예정이다.
- `EventName`은 명세 예시 4개만 둔다.

## 변경 이력
- 2026-11-02: 검증 — V5 적용 후 전체 49개 통과(JSON·`@Version` 매핑 조정 없음). 신청 유니크를 일반 인덱스로 바꾸면 해당 테스트만 실패하는 것을 확인. 로컬 bootRun 후 `/actuator/health` UP
