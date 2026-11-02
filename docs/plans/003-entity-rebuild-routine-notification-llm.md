# 003 엔티티 재구성: routine·notification·llm 모듈 구현 계획

- 명세: docs/db-schema.md (3.3 routine, 3.4 notification, 3.5 llm, 1장 설계 원칙)
- 크기: M — L의 일부 (엔티티 재구성 중 3/4)
  - 001 기반 + user ✅ · 002 workout ✅ · **003 routine·notification·llm** · 004 challenge·공통
- 상태: 승인됨 (2026-11-02)

## Context

001·002에서 Flyway·UTC·`@RepositoryTest` 기반과 user·workout 모듈 테이블을 만들었다. 이번에는 "오늘의 운동" 생성 흐름이 쓸 4개 테이블을 만든다: 루틴(`daily_routine`, `routine_item`), 알림 발송 기록(`push_log`), LLM 호출 로그(`llm_call_log`).
루틴 생성·푸시 발송·LLM 호출 로직은 이번 범위가 아니다. 그 기능이 기대는 **중복 차단 제약**(같은 날 루틴 1개, 같은 대상 알림 1번)을 정확히 만들고 검증한다.

사용자 결정:
- `push_log.ref_id`는 **NOT NULL**로 바꾼다. MySQL 유니크는 NULL끼리 같은 값으로 보지 않아 `(user_id, type, ref_id)`가 ref_id NULL 알림의 중복을 막지 못한다. 1차 알림 종류(DAILY_ROUTINE → 루틴 id, CHALLENGE_PROMOTED → 챌린지 id)는 모두 대상이 있다. 대상 없는 알림이 생기면 그때 새 버전으로 설계한다. `db-schema.md` 3.4도 고친다.

## 접근

002와 같은 방식으로 `V4__create_routine_notification_llm_tables.sql` + 엔티티 + 저장소 + 저장소 테스트를 만든다. 정해진 규칙(ID 참조, FK는 같은 모듈 DDL에만, `Instant`/`LocalDate`, enum은 `VARCHAR` + `EnumType.STRING`, `BaseTimeEntity` 상속)을 그대로 따른다.
새 타입은 `TINYINT`(regen_count, retry_count) 하나다. `UserProfile.workoutDays`처럼 Kotlin `Int` + `@JdbcTypeCode(SqlTypes.TINYINT)`로 맞추고 `SchemaValidationTest`가 확인한다. 모듈 간 참조(user_id, exercise_id, ref_id)는 FK 없이 ID + 인덱스만 둔다.

## 변경 파일

| 파일 | 변경 | 내용 |
| --- | --- | --- |
| db/migration/V4__create_routine_notification_llm_tables.sql | 신규 | 4개 테이블, 유니크·인덱스, routine_item → daily_routine FK |
| routine/domain/DailyRoutine.kt, RoutineItem.kt | 신규 | 루틴, 루틴 항목 |
| routine/domain/RoutineStatus.kt, RoutineSource.kt, SplitDay.kt, RoutineFeedback.kt | 신규 | enum (값은 아래) |
| notification/domain/PushLog.kt, PushType.kt, PushResult.kt | 신규 | 발송 기록 |
| llm/domain/LlmCallLog.kt, LlmFeature.kt, ValidationResult.kt | 신규 | 호출 로그 |
| routine·notification·llm/repository/* | 신규 | `JpaRepository` 4개 (조회 메서드는 기능 구현 때 추가) |
| test/.../support/RoutineFixtures.kt | 신규 | 루틴·항목·알림·LLM 로그 픽스처 |
| test/.../routine/repository/RoutineConstraintTest.kt, RoutineStorageTest.kt | 신규 | 아래 테스트 목록 |
| test/.../notification/repository/PushLogConstraintTest.kt | 신규 | |
| test/.../llm/domain/LlmCallLogTest.kt, llm/repository/LlmCallLogRepositoryTest.kt | 신규 | |
| docs/db-schema.md | 수정 | 3.4 push_log.ref_id NOT NULL과 이유 |
| CLAUDE.md | 수정 | routine·notification·llm "엔티티 구현됨" 표시, 진행 상황 갱신 |

enum 값:
- `RoutineStatus`: PENDING, GENERATED, FALLBACK, FAILED, SKIPPED
- `RoutineSource`: LLM, FALLBACK, REGENERATED
- `SplitDay`: FULL_BODY, UPPER, LOWER, PUSH, PULL, LEGS (`SplitType` 무분할·2분할·3분할의 하루 단위)
- `RoutineFeedback`: EASY, OK, HARD
- `PushType`: DAILY_ROUTINE, CHALLENGE_PROMOTED
- `PushResult`: PENDING, SENT, FAILED, TOKEN_EXPIRED
- `LlmFeature`: ROUTINE (DIET, COACH_CHAT은 2차에 추가)
- `ValidationResult`: PASS, SCHEMA_ERROR, RULE_VIOLATION, TIMEOUT

## 데이터 변경

`docs/db-schema.md` 3.3~3.5를 그대로 만들되 push_log.ref_id만 NOT NULL. 모든 테이블에 `created_at`, `updated_at`.

| 테이블 | PK | 유니크 / 인덱스 | FK (같은 모듈만) |
| --- | --- | --- | --- |
| daily_routine | id | **UK `(user_id, routine_date)`**, IDX `(routine_date, status)` | - (user_id는 모듈 간) |
| routine_item | id | IDX `(routine_id, item_order)` | routine_id → daily_routine |
| push_log | id | **UK `(user_id, type, ref_id)`**, IDX `(result, scheduled_at)` | - |
| llm_call_log | id | IDX `(feature, created_at)`, `(prompt_version, created_at)` | - |

`routine_item.exercise_id`, `alternative_exercise_id`는 workout 모듈 소유라 FK 없음. 존재하는 종목인지는 루틴 생성 시 서버가 후보 목록(`ExerciseService`)으로 검증한다.

## API

해당 없음 (엔티티·저장소만).

## 핵심 코드 스케치

```sql
-- 같은 날 루틴 중복 생성을 DB가 차단 (생성 스케줄러가 두 대에서 돌거나 재시도돼도 1개)
CREATE TABLE daily_routine
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL COMMENT '모듈 간 참조, FK 없음',
    routine_date      DATE         NOT NULL COMMENT '사용자 시간대 기준 날짜',
    status            VARCHAR(20)  NOT NULL COMMENT 'PENDING, GENERATED, FALLBACK, FAILED, SKIPPED',
    source            VARCHAR(20)  NOT NULL COMMENT 'LLM, FALLBACK, REGENERATED',
    split_day         VARCHAR(20)  NOT NULL,
    coach_note        VARCHAR(300) NULL,
    estimated_minutes SMALLINT     NULL,
    prompt_version    VARCHAR(30)  NULL,
    regen_count       TINYINT      NOT NULL COMMENT '"다른 루틴" 요청 횟수 (하루 2회)',
    feedback          VARCHAR(10)  NULL COMMENT 'EASY, OK, HARD',
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_routine_user_date (user_id, routine_date),
    KEY idx_daily_routine_date_status (routine_date, status)
) ...;

-- ref_id NOT NULL: NULL이면 유니크가 중복을 못 막는다. ref_id는 type에 따라 루틴 id 또는 챌린지 id
CREATE TABLE push_log
(
    ...
    type         VARCHAR(30) NOT NULL,
    ref_id       BIGINT      NOT NULL COMMENT 'type별 대상 id (루틴·챌린지). 모듈 간 참조, FK 없음',
    scheduled_at DATETIME(6) NOT NULL,
    sent_at      DATETIME(6) NULL,
    result       VARCHAR(20) NOT NULL,
    opened_at    DATETIME(6) NULL,
    ...
    UNIQUE KEY uk_push_log_user_type_ref (user_id, type, ref_id),
    KEY idx_push_log_result_scheduled (result, scheduled_at)
) ...;
```

```kotlin
// TINYINT는 UserProfile.workoutDays와 같은 방식으로 맞춘다
@JdbcTypeCode(SqlTypes.TINYINT)
@Column(nullable = false)
var regenCount: Int = 0

// LLM 호출 로그: 실패 사유는 외부 응답에서 와서 길이를 알 수 없다.
// 300자를 넘으면 저장 자체가 실패해 로그가 사라지므로 엔티티에서 자른다.
class LlmCallLog(
    ...
    failureReason: String? = null,
) : BaseTimeEntity() {
    @Column(length = FAILURE_REASON_MAX)
    val failureReason: String? = failureReason?.take(FAILURE_REASON_MAX)

    companion object { const val FAILURE_REASON_MAX = 300 }
}
```

`coach_note`(VARCHAR(300))는 자르지 않는다. LLM 출력이라 길이 초과는 검증 실패로 처리해 대체 응답을 쓰는 게 맞다 (루틴 생성 기능에서).

## 정합성·동시성·멱등성

- **루틴 중복 생성:** `(user_id, routine_date)` 유니크가 최후 안전망. 생성 스케줄러가 여러 대에서 돌거나 재시도돼도 하루 1개. 충돌 시 기존 행을 쓰는 처리는 루틴 생성 기능에서.
- **알림 중복 발송:** `(user_id, type, ref_id)` 유니크 + ref_id NOT NULL. 같은 루틴에 알림 행이 두 번 생기지 않는다. 범위에 type이 있어 루틴 id와 챌린지 id가 우연히 같아도 충돌하지 않는다.
- **"다른 루틴" 하루 2회:** 동시 요청 시 읽고-쓰기로 하면 한도를 넘을 수 있다. 조건부 UPDATE(`WHERE regen_count < 2`) 같은 처리는 재생성 기능 계획에서 정한다. 이번에는 컬럼만.
- **루틴 항목 정합성:** `routine_item.routine_id` FK로 없는 루틴의 항목은 저장되지 않는다.

## 실패·대체 경로

해당 없음 (외부 호출 없음). 단, LLM 실패 사유가 길어 로그 저장이 실패하는 경로는 위처럼 엔티티에서 막는다.

## 테스트 목록

저장소 테스트는 모두 `@RepositoryTest`. `SchemaValidationTest`는 V4가 추가되면 새 테이블까지 자동 검증한다 (TINYINT·BOOLEAN 매핑 포함).

routine
- [ ] `같은 사용자가 같은 날짜에 루틴을 두 번 저장하면 유니크 제약 위반 예외가 난다`
- [ ] `다른 사용자는 같은 날짜에 루틴을 저장할 수 있다` — 유니크 범위가 사용자 단위인지
- [ ] `존재하지 않는 루틴 id로 항목을 저장하면 외래 키 위반 예외가 난다`
- [ ] `루틴 날짜는 시간대 변환 없이 저장된다` — `2026-11-02` → DB 원본 `2026-11-02` (Asia/Seoul JVM)

notification
- [ ] `같은 사용자에게 같은 종류·대상의 알림을 두 번 저장하면 유니크 제약 위반 예외가 난다`
- [ ] `종류가 다르면 같은 대상 id로 알림을 저장할 수 있다` — 루틴 5번과 챌린지 5번
- [ ] `발송 예정 시각은 UTC로 저장된다` — `2026-11-01T22:30:00Z` → DB 원본 `2026-11-01 22:30:00.000000` (KST로 밀리면 다음 날 07:30)

llm
- [ ] `실패 사유가 300자를 넘으면 300자로 잘라 둔다` (단위)
- [ ] `평가 실행 로그는 사용자 없이 저장할 수 있다` — user_id·ref_id NULL 허용

## 작업 순서

1. enum·엔티티·저장소 뼈대 (컴파일)
2. **테스트 작성** → V4가 없어 실패 확인 (`missing table`)
3. `V4__create_routine_notification_llm_tables.sql` → 테스트 통과 (매핑 어긋남은 이 단계에서 조정)
4. 검증 테스트가 잡는지 확인: 두 유니크 제약을 하나씩 빼면 해당 테스트가 실패하는지
5. `./gradlew bootRun`으로 로컬 `create` 기동 확인
6. `db-schema.md`, CLAUDE.md 갱신 → `/review-change` → 커밋 (main)

## 검증

- `./gradlew test`: 기존 테스트 + 새 테스트 9개 통과
- `./gradlew bootRun`: 로컬에서 정상 기동, `/actuator/health` UP

## 확인 필요

- `SplitDay` 값(FULL_BODY, UPPER, LOWER, PUSH, PULL, LEGS)은 분할 방식의 일반적인 구성으로 정했어요. 루틴 생성 규칙을 짤 때 바뀔 수 있고, VARCHAR라 값 추가는 마이그레이션 없이 가능해요.
- `LlmFeature`는 1차에 쓰는 ROUTINE만 두고, DIET·COACH_CHAT은 2차에 추가해요.

## 변경 이력
- 2026-11-02: `평가 실행 로그는 사용자 없이 저장할 수 있다` 테스트는 `SELECT user_id`를 읽으면 Spring의 Kotlin 확장 `queryForObject`가 null을 돌려줄 수 없어 NPE가 난다. `SELECT user_id IS NULL`로 DB에서 NULL 여부를 계산해 읽도록 바꿈
- 2026-11-02: 검증 — 두 유니크 제약을 V4에서 하나씩 일반 인덱스로 바꾸면 해당 테스트만 실패하는 것을 확인. 전체 40개 통과, 로컬 bootRun 후 `/actuator/health` UP
- 2026-11-02 (리뷰 반영): `take(300)`은 UTF-16 단위라 300번째 위치의 이모지(서로게이트 쌍)를 반으로 자를 수 있다. 짝 없는 앞쪽이 남으면 한 칸 더 빼도록 고치고 경계 테스트 1개 추가 (총 41개)
