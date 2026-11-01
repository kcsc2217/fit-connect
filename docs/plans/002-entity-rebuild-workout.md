# 002 엔티티 재구성: workout 모듈 구현 계획

- 명세: docs/db-schema.md (3.2 workout 모듈, 1장 설계 원칙)
- 크기: L의 일부 (엔티티 재구성 중 2/4)
  - 001 기반 + user ✅ · **002 workout** · 003 routine·notification·llm · 004 challenge·공통
- 상태: 승인됨 (2026-11-01)

## Context

001에서 Flyway·UTC·`@RepositoryTest` 기반과 user 모듈을 만들었다. 이번에는 운동 기록의 중심인 workout 모듈 6개 테이블을 만든다.
이 모듈에는 앞으로 이력서 핵심 성과가 될 부분이 들어간다: 오프라인 동기화 멱등성(`(user_id, client_id)` 유니크, `client_id BINARY(16)`)과 LLM에 넘길 후보 종목 필터(존재하지 않는 종목 생성 0건).
동기화 API·upsert 로직은 이번 범위가 아니다. 그 기능이 올라갈 테이블과 제약을 정확히 만들고 검증한다.

사용자 결정:
- 초기 종목 데이터는 Flyway 마이그레이션(V3)으로 넣고, **로컬에서도 같은 SQL 파일을 실행**한다 (데이터 원본 하나).
- 맨몸 종목은 **필요 장비 없음**(exercise_equipment 행 없음) → 장비와 관계없이 항상 후보.
- **후보 종목 필터 쿼리를 이번에 포함**한다. 사용자 장비 목록을 인자로 받아 user 모듈 테이블을 조인하지 않는다.

## 접근

001과 같은 방식으로 `V2__create_workout_tables.sql` + 엔티티 + 저장소 + 저장소 테스트를 만든다. 001에서 정한 규칙(ID 참조, FK는 같은 모듈 DDL에만, `Instant`/`LocalDate`, `@Embeddable` 복합 키)을 그대로 따른다.
새로 등장하는 타입(`BINARY(16)` UUID, `BOOLEAN`, `DECIMAL(6,2)`, `DATE`)은 `SchemaValidationTest`와 **DB 원본 값을 읽는 테스트**로 매핑을 확인한다(ADR 0002의 약속).
`Experience` enum은 user(경력)와 workout(종목 권장 최소 경력)이 함께 쓰므로 `Equipment`처럼 `common/type`으로 옮긴다 (workout이 user 모듈 enum에 의존하지 않도록).

## 변경 파일

| 파일 | 변경 | 내용 |
| --- | --- | --- |
| db/migration/V2__create_workout_tables.sql | 신규 | 6개 테이블, 유니크·인덱스·같은 모듈 FK |
| db/migration/V3__seed_exercises.sql | 신규 | 초기 종목 약 60개 + 필요 장비 |
| common/type/Experience.kt | 이동 | user/domain → common/type (UserProfile import 수정) |
| workout/domain/BodyPart.kt, LoadType.kt | 신규 | CHEST·BACK·SHOULDER·LEGS·ARMS·CORE / BARBELL·DUMBBELL·MACHINE·BODYWEIGHT |
| workout/domain/Exercise.kt, ExerciseEquipment(+Id).kt | 신규 | 종목 사전, 필요 장비(복합 키) |
| workout/domain/WorkoutSession.kt, WorkoutSet.kt | 신규 | `clientId: UUID`, `deleted`, `clientUpdatedAt` |
| workout/domain/PersonalRecord(+Id).kt, WeeklyVolume(+Id).kt | 신규 | 사전 집계(복합 키) |
| workout/repository/* | 신규 | 6개 저장소. `ExerciseRepository.findCandidates(...)` 포함 |
| application-local.yml | 수정 (미커밋) | 로컬 기동 시 V3 실행 설정 |
| test/.../support/WorkoutFixtures.kt | 신규 | 세션·세트·종목 픽스처 |
| test/.../workout/repository/*Test.kt | 신규 | 아래 테스트 목록 |
| CLAUDE.md | 수정 | 로컬 시드 방식, 맨몸 종목 규칙, workout "구현됨" 표시 |

## 데이터 변경

`docs/db-schema.md` 3.2를 그대로 만든다. 모든 테이블에 `created_at`, `updated_at`.

| 테이블 | PK | 유니크 / 인덱스 | FK (같은 모듈만) |
| --- | --- | --- | --- |
| exercise | id | UK `(code)`, IDX `(body_part, active)` | - |
| exercise_equipment | (exercise_id, equipment) | - | exercise_id → exercise |
| workout_session | id | **UK `(user_id, client_id)`**, IDX `(user_id, started_at)` | - (user_id는 모듈 간 참조) |
| workout_set | id | **UK `(user_id, client_id)`**, IDX `(session_id)`, `(user_id, exercise_id, performed_at)` | session_id → workout_session, exercise_id → exercise |
| personal_record | (user_id, exercise_id) | - | - (명세대로 집계 테이블은 FK 없음) |
| weekly_volume | (user_id, week_start, body_part) | - | - |

- 초기 종목: 부위 6개에 걸쳐 약 60개 (가슴·등·다리 각 12개 안팎, 어깨·팔 각 8개 안팎, 코어 6개 안팎). 맨몸 종목은 필요 장비 행 없음. 여러 장비가 필요한 종목(벤치프레스 = BARBELL + BENCH)은 행 여러 개.
- 로컬: `application-local.yml`에 아래 설정을 추가해, Hibernate가 테이블을 만든 직후 같은 V3 파일을 실행한다.

## API

해당 없음 (엔티티·저장소·조회 쿼리만).

## 핵심 코드 스케치

```sql
-- V2 일부: 멱등성 핵심. 유니크 범위는 사용자 단위
CREATE TABLE workout_set (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    user_id           BIGINT        NOT NULL,
    client_id         BINARY(16)    NOT NULL COMMENT '앱이 만든 UUID',
    session_id        BIGINT        NOT NULL,
    exercise_id       BIGINT        NOT NULL,
    exercise_order    SMALLINT      NOT NULL,
    set_order         SMALLINT      NOT NULL,
    weight_kg         DECIMAL(6, 2) NOT NULL COMMENT '맨몸은 0',
    reps              SMALLINT      NOT NULL,
    rpe               DECIMAL(3, 1) NULL,
    completed         BOOLEAN       NOT NULL,
    deleted           BOOLEAN       NOT NULL COMMENT 'tombstone',
    performed_at      DATETIME(6)   NOT NULL,
    client_updated_at DATETIME(6)   NOT NULL COMMENT 'last-write-wins 기준',
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_workout_set_user_client (user_id, client_id),
    KEY idx_workout_set_session (session_id),
    KEY idx_workout_set_user_exercise_performed (user_id, exercise_id, performed_at),
    CONSTRAINT fk_workout_set_session FOREIGN KEY (session_id) REFERENCES workout_session (id),
    CONSTRAINT fk_workout_set_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id)
) ...;

-- V3 일부: 장비는 code로 종목 id를 찾아 넣는다 (AUTO_INCREMENT 값에 의존하지 않음)
INSERT INTO exercise (code, name_ko, body_part, load_type, min_level, compound, default_rest_sec, active, created_at, updated_at)
VALUES ('BARBELL_BENCH_PRESS', '바벨 벤치프레스', 'CHEST', 'BARBELL', 'BEGINNER', TRUE, 120, TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)), ...;
INSERT INTO exercise_equipment (exercise_id, equipment, created_at, updated_at)
SELECT id, 'BARBELL', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) FROM exercise WHERE code = 'BARBELL_BENCH_PRESS';
```

```kotlin
// 세트: 앱이 만든 UUID를 BINARY(16)으로. 연관관계 없이 ID만
@Entity
@Table(name = "workout_set", uniqueConstraints = [UniqueConstraint(name = "uk_workout_set_user_client", columnNames = ["user_id", "client_id"])], indexes = [...])
class WorkoutSet(
    @Column(nullable = false) val userId: Long,
    @Column(nullable = false, columnDefinition = "binary(16)") val clientId: UUID,
    @Column(nullable = false) val sessionId: Long,
    @Column(nullable = false) val exerciseId: Long,
    @Column(nullable = false, precision = 6, scale = 2) var weightKg: BigDecimal,
    @Column(precision = 3, scale = 1) var rpe: BigDecimal?,
    @Column(nullable = false) var deleted: Boolean = false,
    @Column(nullable = false) var clientUpdatedAt: Instant,
    // ...
) : BaseTimeEntity()

// 추정 1RM: 이름 규칙상 est1rm → "est1rm"이 되므로 명시
@Column(name = "est_1rm", nullable = false, precision = 6, scale = 2) var est1rm: BigDecimal
```

```kotlin
// 후보 종목: 필요한 장비를 모두 가진 활성 종목. 맨몸 종목(필요 장비 없음)은 항상 포함.
// 사용자 장비는 호출하는 쪽(routine)이 user 서비스에서 받아 넘긴다 → user 모듈 테이블을 조인하지 않음
interface ExerciseRepository : JpaRepository<Exercise, Long> {

    @Query("""
        SELECT e FROM Exercise e
        WHERE e.active = true AND e.bodyPart IN :bodyParts
          AND NOT EXISTS (
            SELECT 1 FROM ExerciseEquipment ee
            WHERE ee.id.exerciseId = e.id AND ee.id.equipment NOT IN :equipments)
    """)
    fun findCandidatesOwning(bodyParts: Collection<BodyPart>, equipments: Collection<Equipment>): List<Exercise>

    @Query("""
        SELECT e FROM Exercise e
        WHERE e.active = true AND e.bodyPart IN :bodyParts
          AND NOT EXISTS (SELECT 1 FROM ExerciseEquipment ee WHERE ee.id.exerciseId = e.id)
    """)
    fun findBodyweightCandidates(bodyParts: Collection<BodyPart>): List<Exercise>

    // 장비 목록이 비면 NOT IN ()이 잘못된 SQL이 되므로 맨몸 종목 쿼리로 분기
    fun findCandidates(bodyParts: Collection<BodyPart>, equipments: Collection<Equipment>): List<Exercise> =
        if (equipments.isEmpty()) findBodyweightCandidates(bodyParts)
        else findCandidatesOwning(bodyParts, equipments)
}
```

```yaml
# application-local.yml 추가: create로 테이블을 만든 뒤 같은 시드 파일 실행
spring:
  jpa.defer-datasource-initialization: true
  sql.init:
    mode: always
    encoding: UTF-8
    data-locations: classpath:db/migration/V3__seed_exercises.sql
```

**타입 매핑 주의:** `BOOLEAN`(MySQL에서는 `TINYINT(1)`)과 Kotlin `Boolean`, `BINARY(16)`과 `UUID`가 validate에서 어긋날 수 있다. 실패하면 001처럼 `@JdbcTypeCode`로 맞춘다.

## 정합성·동시성·멱등성

- **동기화 멱등성:** `workout_session`, `workout_set`의 `(user_id, client_id)` 유니크가 재전송·동시 전송의 최후 안전망이다. 범위가 사용자 단위라, 다른 사용자가 우연히 같은 UUID를 써도 충돌하지 않는다. upsert 로직은 동기화 기능에서 만든다.
- **삭제:** 실제로 지우지 않고 `deleted = true`(tombstone). 늦게 도착한 수정이 삭제된 기록을 되살리지 않게 하기 위한 컬럼이고, 판정 로직은 동기화 기능 범위.
- **종목 코드 중복:** `exercise (code)` 유니크. LLM 출력과 평가가 code로 종목을 찾는다.
- **존재하지 않는 종목:** 세트의 `exercise_id`에 FK가 있어 사전에 없는 종목으로 기록할 수 없다.

## 실패·대체 경로

해당 없음 (외부 호출 없음).

## 테스트 목록

모두 `@RepositoryTest`. `SchemaValidationTest`는 V2가 추가되면 자동으로 새 테이블까지 검증한다.

멱등성·제약
- [ ] `같은 사용자가 같은 client_id로 세트를 두 번 저장하면 유니크 제약 위반 예외가 난다`
- [ ] `다른 사용자는 같은 client_id로 세트를 저장할 수 있다` — 유니크 범위가 사용자 단위인지
- [ ] `같은 사용자가 같은 client_id로 세션을 두 번 저장하면 유니크 제약 위반 예외가 난다`
- [ ] `존재하지 않는 세션 id로 세트를 저장하면 외래 키 위반 예외가 난다`
- [ ] `같은 종목 코드를 두 번 저장하면 유니크 제약 위반 예외가 난다`

타입 저장 (DB 원본 값 확인)
- [ ] `client_id는 UUID 16바이트 그대로 저장된다` — `HEX(client_id)`가 UUID의 하이픈을 뺀 값과 같은지
- [ ] `무게는 소수 둘째 자리까지 오차 없이 저장된다` — 82.5 → `82.50`
- [ ] `주 시작일은 시간대 변환 없이 저장된다` — `2026-11-02` → `2026-11-02` (Asia/Seoul JVM)

초기 종목 데이터
- [ ] `초기 종목 데이터는 50개 이상이고 모든 부위를 포함한다` — `findAll()`로 전부 엔티티로 읽히는지(enum 값 오타 검출) 포함

후보 종목 필터
- [ ] `필요한 장비 중 하나라도 없으면 후보에서 빠진다` — BARBELL만 있으면 BARBELL + BENCH 종목은 제외, BARBELL 단독 종목은 포함
- [ ] `비활성 종목은 후보에서 빠진다`
- [ ] `보유 장비가 없으면 맨몸 종목만 후보가 된다` — 빈 목록 경계

## 작업 순서

1. `Experience`를 `common/type`으로 이동 → 기존 테스트 통과 확인
2. workout enum·엔티티·저장소 뼈대 (컴파일)
3. **테스트 작성** → V2가 없어 실패 확인 (`missing table`)
4. `V2__create_workout_tables.sql` → 테스트 통과 (타입 매핑 어긋남은 이 단계에서 조정)
5. `V3__seed_exercises.sql` + 시드·후보 필터 테스트 통과
6. 검증 테스트가 잡는지 확인: `(user_id, client_id)` 유니크를 빼면 실패하는지
7. 로컬 시드 설정 → `bootRun`에서 종목이 들어가는지 확인
8. CLAUDE.md 갱신 → `/review-change` → 커밋 (main)

## 검증

- `./gradlew test`: 기존 12개 + 새 테스트 12개 통과
- `./gradlew bootRun`: 로컬에서 `create` 후 V3 시드가 실행되고 정상 기동, `/actuator/health` UP
- 로컬 `fitness` DB의 `exercise` 행 수가 시드와 같은지 확인 (읽기만)

## 확인 필요

- 초기 종목 목록(약 60개)은 제가 일반적인 헬스장 종목으로 만들어요. 구현 후 V3 파일을 보고 빼거나 추가할 종목을 알려주세요. (db-schema 열린 질문 "1차 종목 사전 규모")
- `Equipment.BODYWEIGHT` 값은 남겨 두되 종목에는 쓰지 않아요. 온보딩 선택지에서 뺄지는 온보딩 API에서 정해요.

## 변경 이력

- 2026-11-01: 초기 종목은 Claude가 만들지 않고 사용자가 free-exercise-db에서 고른 CSV 2개(종목 65개, 필요 장비 79행)를 쓴다. CSV는 `docs/data/`에 두고, `scripts/generate_exercise_seed.py`로 V3 SQL을 생성한다
- 2026-11-01: CSV에 맞춰 장비 enum에 RACK, DIP_BAR 추가. exercise 테이블에 name_en(영문 이름), source_id(free-exercise-db 원본 ID) 컬럼 추가. db-schema.md도 갱신
- 2026-11-01: CSV의 load_type은 BARBELL·DUMBBELL·MACHINE·BODYWEIGHT만 쓴다(케이블은 MACHINE, 케틀벨은 DUMBBELL로 분류됨). 과부하 규칙의 증가폭 분류라 그대로 둔다
- 2026-11-01: 후보 필터의 분기(장비 목록이 비면 맨몸 쿼리)를 저장소 기본 메서드 대신 `ExerciseService`에 둠. Kotlin 인터페이스 기본 메서드는 별도 컴파일 옵션(`-Xjvm-default`) 없이는 Spring Data가 쿼리 메서드로 오인한다. 다른 모듈은 어차피 서비스로만 접근해야 하므로 서비스가 공개 창구가 된다. 부위 목록이 비면(`IN ()`도 잘못된 SQL) 바로 빈 결과를 돌려주는 처리와 테스트 1개를 추가
- 2026-11-01: 로컬 시드 설정(sql.init, defer-datasource-initialization)이 local,test 프로파일로 테스트에 상속돼 Flyway와 순환 의존이 생김. application-test.yml에서 두 설정을 끔
- 2026-11-01: V1의 user_equipment 장비 주석에 RACK, DIP_BAR를 추가해 V1 체크섬이 바뀜. 운영·스테이징에 배포한 적이 없어 영향 없음 (배포 후에는 적용된 파일을 고치지 않는다)
- 2026-11-01 (리뷰 반영): 후보 필터 테스트 4개 추가(장비가 있어도 맨몸 포함, 다른 부위 제외 2개, 비활성 맨몸 제외). 시드 테스트를 개수 대신 CSV 내용 전체와 비교하도록 변경(종목 코드 집합, (코드, 장비) 쌍 집합). 생성 스크립트에 검사 추가: 맨몸이 아닌 종목의 장비 누락, 빈 값, 코드 형식·길이, 휴식 시간 범위. 쿼리 조건을 일부러 고장 내거나 V3에서 장비 줄을 지우면 해당 테스트가 실패하는 것을 확인
