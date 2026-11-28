# 011 운동 기록 동기화 서버 API 구현 계획

- 명세: docs/specs/workout-logging.md (확정 2026-11-24)
- 크기: L의 일부 (운동 기록 중 1/2. 다음은 012 앱 기록 화면·기기 저장소·자동 전송)
- 상태: 승인됨 (2026-11-28)
- 브랜치: `feat/workout-sync-server` (`feat/onboarding-server`에서 분기. 009의 `InvalidInputException`·Jackson 설정을 쓰므로. 009가 main에 머지되면 main 기준으로 다시 맞춘다)

## Context

앱이 오프라인에서 쌓은 운동 기록(세션·세트)을 올릴 API가 없다. 테이블·엔티티(`workout_session`, `workout_set`, 유니크 `(user_id, client_id)`)는 V2(계획 002)에 있다. 이번 작업에서 묶음 동기화 API(최대 100건, 멱등 upsert, last-write-wins, tombstone)와 앱이 종목을 고를 종목 사전 조회 API를 만든다. 이 기록이 이후 오늘의 운동(무게 계산)과 FR-24 집계의 입력이 된다.

## 접근

- **항목별 upsert를 SQL로 직접 쓴다 (`JdbcTemplate`).** 같은 항목이 동시에 두 번 와도 행이 하나여야 하고(유니크 위반 500 금지), 항목마다 결과(`APPLIED`/`IGNORED`/`FAILED`)를 알려야 한다. JPA `save`는 중복 INSERT 예외가 나면 영속성 컨텍스트가 깨지고 트랜잭션이 롤백 전용이 돼 묶음 전체가 실패한다. 그래서 항목마다 이렇게 한다:
  1. 서버에 없는 항목: `INSERT` 시도 → 성공하면 `APPLIED`
  2. `DuplicateKeyException`(이미 있음, 또는 다른 요청이 방금 넣음): 조건부 `UPDATE ... WHERE deleted = 0 AND client_updated_at < :new` → 1행이면 `APPLIED`, 0행이면 `IGNORED`
  - MySQL은 유니크 위반을 그 문장만 되돌리고 트랜잭션은 살려 두므로, 예외를 잡고 같은 트랜잭션에서 계속 진행할 수 있다. 다른 트랜잭션이 같은 키를 넣고 커밋 전이면 INSERT가 기다렸다가 중복 예외를 받으므로 경쟁에서도 행은 하나다.
  - `INSERT ... ON DUPLICATE KEY UPDATE` 한 문장은 쓰지 않는다. 대입이 왼쪽부터 순서대로 적용돼 `deleted`·`client_updated_at`을 조건과 대입에 함께 쓰면 순서에 따라 결과가 틀어지고, Connector/J 기본 설정에서는 영향 행 수로 "새로 넣음"과 "그대로"를 구분할 수 없다.
- **묶음 앞에서 한 번에 읽어 왕복을 줄인다.** 이미 있는 `client_id`를 한 쿼리로 읽어 두고, 있는 항목은 바로 UPDATE로 간다(예외 경로는 경쟁 때만). 활성 종목 id도 한 쿼리로 읽어 둔다. 100건이면 대략 SELECT 3번 + 항목당 문장 1번.
- **세트는 세션을 `sessionClientId`로 가리킨다.** 세션들을 먼저 처리한 뒤, 세트가 가리키는 세션 client_id들을 `(user_id, client_id)`로 한 번에 찾아 `session_id`를 채운다. 없으면 그 세트만 `SESSION_NOT_FOUND`. 세트의 `session_id`·`exercise_id`는 만든 뒤 바꾸지 않는다(UPDATE 대상에서 뺌).
- **검증은 항목 단위.** 묶음 크기(1~100)와 JSON 형식만 요청 전체를 400으로 막고, 값 범위·종목 존재는 그 항목만 `FAILED`로 돌려준다. 그래서 항목 DTO에는 Bean Validation을 걸지 않고 서비스의 검증기가 이유 코드를 만든다.
- **한 묶음 = 한 트랜잭션.** 검증 실패는 예외가 아니라 결과라서 다른 항목을 막지 않는다. 데드락을 줄이려고 항목을 `client_id` 순으로 처리한다.

## 변경 파일

| 파일 | 변경 | 내용 |
| --- | --- | --- |
| workout/controller/WorkoutSyncController.kt | 신규 | `POST /api/v1/workouts/sync` |
| workout/controller/ExerciseController.kt | 신규 | `GET /api/v1/exercises` |
| workout/service/WorkoutSyncService.kt | 신규 | 묶음 검증 → 사용자 확인 → 세션 처리 → 세트 처리 → 결과 |
| workout/service/SyncItemValidator.kt | 신규 | 항목 값 검증 → 실패 이유 (순수 함수) |
| workout/repository/WorkoutSyncJdbcRepository.kt | 신규 | INSERT·조건부 UPDATE·묶음 조회 (`NamedParameterJdbcTemplate`), UUID ↔ BINARY(16) 변환 |
| workout/dto/SyncDtos.kt | 신규 | `SyncRequest(sessions, sets)`, `SessionChange`, `SetChange`, `SyncResponse(results)`, `ItemResult`, `Outcome`, `FailReason` |
| workout/dto/ExerciseDtos.kt | 신규 | `ExerciseResponse(id, code, nameKo, bodyPart, loadType, defaultRestSec)` |
| workout/service/ExerciseService.kt | 수정 | `findAllActive()`, `activeIds()` 추가 |
| workout/repository/ExerciseRepository.kt | 수정 | `findAllByActiveTrueOrderByBodyPartAscIdAsc()`, 활성 id 조회 |
| user/service/UserService.kt | 수정 | `requireRecordable(userId)`: 회원 없음 404, 탈퇴 403 (workout이 user 저장소를 직접 쓰지 않게) |
| test/.../workout/controller/WorkoutSyncControllerTest.kt, ExerciseControllerTest.kt | 신규 | 통합 |
| test/.../workout/service/WorkoutSyncConcurrencyTest.kt | 신규 | 동시성 |
| test/.../workout/service/ExerciseServiceTest.kt | 수정 | 비활성 종목 제외 (`@RepositoryTest`, 롤백) |
| test/.../support/SyncTestSupport.kt | 신규 | 요청 본문 만들기 (세션·세트 기본값 + 바꿔 끼우기) |
| CLAUDE.md | 수정 | 진행 상황, workout 구현 표시, 동기화 upsert 방식 한 줄 |

## 데이터 변경

없음. 기존 `workout_session`·`workout_set`(유니크 `(user_id, client_id)`, 세트→세션 FK), `exercise`를 쓴다. 세트에는 `server_received_at`이 없지만(세션에만) 이번에는 추가하지 않는다. 세트 수신 시각은 같은 묶음의 세션 수신 시각으로 충분하다.

## API

| 메서드 | 경로 | 요청 | 응답 | 에러 |
| --- | --- | --- | --- | --- |
| POST | /api/v1/workouts/sync | `{sessions: [...], sets: [...]}` 합계 1~100건 | 200 `{results: [{clientId, type, outcome, reason?}]}` (요청 순서대로) | 400 `INVALID_INPUT`(건수·형식), 401, 403 `FORBIDDEN`(탈퇴), 404 `NOT_FOUND`(회원 없음) |
| GET | /api/v1/exercises | - | 200 `[{id, code, nameKo, bodyPart, loadType, defaultRestSec}]` 부위·id 순 | 401 |

- `outcome`: `APPLIED`(새로 넣음·덮어씀), `IGNORED`(더 새로운 값이 있거나 삭제된 행), `FAILED`
- `reason`(FAILED일 때): `INVALID_VALUE`, `UNKNOWN_EXERCISE`, `SESSION_NOT_FOUND`
- 세션 필드: `clientId, startedAt, endedAt?, conditionNote?, deleted, clientUpdatedAt`. 세트 필드: `clientId, sessionClientId, exerciseId, exerciseOrder, setOrder, weightKg, reps, rpe?, completed, deleted, performedAt, clientUpdatedAt`. 시각은 ISO-8601 UTC(`Instant`).

## 핵심 코드 스케치

### 서비스 (WorkoutSyncService)

```kotlin
@Transactional
fun sync(userId: Long, request: SyncRequest): SyncResponse {
    val total = request.sessions.size + request.sets.size
    if (total !in 1..MAX_ITEMS) throw InvalidInputException("items", "한 번에 1~${MAX_ITEMS}건만 보낼 수 있습니다")
    userService.requireRecordable(userId)                       // 404 / 403

    val now = clock.instant()
    val results = LinkedHashMap<UUID, ItemResult>()             // 요청 순서 유지

    // 1) 세션 (client_id 순으로 처리해 데드락을 줄인다)
    val existingSessions = repo.findSessionClientIds(userId, request.sessions.map { it.clientId })
    for (s in request.sessions.sortedBy { it.clientId }) {
        val reason = validator.session(s)
        results[s.clientId] = if (reason != null) failed(s, reason)
            else upsert(s.clientId in existingSessions,
                insert = { repo.insertSession(userId, s, clamp(s.clientUpdatedAt, now), now) },
                update = { repo.updateSession(userId, s, clamp(s.clientUpdatedAt, now), now) })
    }

    // 2) 세트: 가리키는 세션 id를 한 번에 찾고, 활성 종목 id도 한 번에 읽는다
    val sessionIds = repo.findSessionIds(userId, request.sets.map { it.sessionClientId }.toSet())  // client_id → id
    val activeExercises = exerciseService.activeIds()
    val existingSets = repo.findSetClientIds(userId, request.sets.map { it.clientId })
    for (t in request.sets.sortedBy { it.clientId }) {
        val sessionId = sessionIds[t.sessionClientId]
        val reason = validator.set(t) ?: when {
            t.exerciseId !in activeExercises -> FailReason.UNKNOWN_EXERCISE
            sessionId == null -> FailReason.SESSION_NOT_FOUND
            else -> null
        }
        results[t.clientId] = if (reason != null) failed(t, reason)
            else upsert(t.clientId in existingSets,
                insert = { repo.insertSet(userId, sessionId!!, t, clamp(t.clientUpdatedAt, now)) },
                update = { repo.updateSet(userId, t, clamp(t.clientUpdatedAt, now)) })
    }
    return SyncResponse.inRequestOrder(request, results)
}

// 없을 것 같으면 INSERT를 먼저 시도하고, 다른 요청이 먼저 넣었으면(중복 키) 조건부 UPDATE로 넘어간다
private fun upsert(exists: Boolean, insert: () -> Unit, update: () -> Int): Outcome {
    if (!exists) {
        try { insert(); return Outcome.APPLIED } catch (e: DuplicateKeyException) { /* 경쟁: 아래로 */ }
    }
    return if (update() == 1) Outcome.APPLIED else Outcome.IGNORED
}

// 미래로 틀어진 기기 시계가 이후 모든 수정을 이기지 않게 (명세)
private fun clamp(clientUpdatedAt: Instant, now: Instant) =
    if (clientUpdatedAt > now.plus(MAX_CLOCK_SKEW)) now else clientUpdatedAt   // 5분
```

### SQL (WorkoutSyncJdbcRepository)

```sql
-- 세트 덮어쓰기: 삭제된 행은 되살리지 않고, 더 새로운 수정만 이긴다. session_id·exercise_id는 바꾸지 않는다
UPDATE workout_set
SET exercise_order = :exerciseOrder, set_order = :setOrder, weight_kg = :weightKg, reps = :reps, rpe = :rpe,
    completed = :completed, deleted = :deleted, performed_at = :performedAt,
    client_updated_at = :clientUpdatedAt, updated_at = :now
WHERE user_id = :userId AND client_id = :clientId
  AND deleted = 0 AND client_updated_at < :clientUpdatedAt

-- 세트가 가리키는 세션 찾기 (같은 사용자 것만)
SELECT client_id, id FROM workout_session WHERE user_id = :userId AND client_id IN (:clientIds)
```

- WHERE의 `client_updated_at < :new`이 참일 때만 1행이 걸리므로, Connector/J 기본(찾은 행 수 반환)에서도 1이면 실제로 덮어쓴 것이다.
- UUID는 `ByteBuffer`로 상위 8바이트·하위 8바이트를 이어 16바이트로 바꾼다 (Hibernate의 BINARY(16) 매핑과 같은 순서. `WorkoutStorageTest`가 HEX로 확인한 형식).
- 시각은 `Timestamp.from(instant)`로 넘긴다. DataSource의 `connectionTimeZone=UTC`가 JdbcTemplate에도 적용돼 UTC로 저장된다 (원본 값 테스트로 확인).
- `created_at`·`updated_at`은 JPA Auditing을 거치지 않으므로 INSERT·UPDATE에 `Clock` 값을 직접 넣는다.

### 검증 (SyncItemValidator, 실패 이유만 돌려준다)

```kotlin
fun set(t: SetChange): FailReason? = when {
    t.weightKg < BigDecimal.ZERO || t.weightKg > BigDecimal("999.99") || t.weightKg.scale() > 2 -> INVALID_VALUE
    t.reps !in 1..999 -> INVALID_VALUE
    t.rpe != null && (t.rpe < ONE || t.rpe > TEN || t.rpe.remainder(HALF).signum() != 0) -> INVALID_VALUE
    t.exerciseOrder < 0 || t.setOrder < 0 -> INVALID_VALUE
    else -> null
}
fun session(s: SessionChange): FailReason? = when {
    s.endedAt != null && s.endedAt < s.startedAt -> INVALID_VALUE
    (s.conditionNote?.length ?: 0) > 200 -> INVALID_VALUE
    else -> null
}
```

## 정합성·동시성·멱등성

- **재전송:** 같은 항목이 다시 오면 이미 있으니 UPDATE로 가고, 수정 시각이 같아 0행 → `IGNORED`. 행은 늘지 않는다.
- **동시 전송(같은 항목):** 두 요청이 모두 "없음"으로 보고 INSERT하면 한쪽은 유니크 잠금에서 기다렸다가 중복 키 예외 → 조건부 UPDATE. 유니크 위반 500이 나지 않고 행은 하나다 (동시성 테스트).
- **데드락:** 겹치는 묶음 두 개가 서로 다른 순서로 같은 키를 잠그면 데드락이 날 수 있다. 항목을 `client_id` 순으로 처리해 잠금 순서를 맞춘다. 그래도 나면 MySQL이 한쪽을 되돌리고 그 요청은 500 → 앱이 다시 보낸다(멱등이라 안전).
- **소유권:** 모든 조회·갱신에 `user_id = :userId`(토큰)를 건다. 다른 사용자의 세션을 가리키는 세트는 `SESSION_NOT_FOUND`.
- **tombstone:** UPDATE의 `deleted = 0` 조건으로 삭제된 행에는 어떤 수정도 들어가지 않는다.
- **서버 메모리 상태:** 없다.

## 실패·대체 경로

외부 호출 없음. 묶음 중간에 예상하지 못한 DB 예외(데드락 등)가 나면 트랜잭션 전체가 롤백되고 500. 앱은 같은 묶음을 다시 보내며, 멱등이라 결과가 같다.

## 테스트 목록

통합 (`WorkoutSyncControllerTest`, `@IntegrationTest`, 테스트마다 새 개발용 회원과 새 UUID)
- [ ] 1. `처음 보낸 세션과 세트를 저장하고 모두 APPLIED로 응답한다`
- [ ] 2. `같은 묶음을 다시 보내면 행이 늘지 않고 IGNORED로 응답한다`
- [ ] 3. `더 새로운 수정 시각의 세트는 새 값으로 덮어쓴다`
- [ ] 4. `더 오래된 수정 시각의 세트는 무시하고 기존 값을 유지한다`
- [ ] 5. `삭제한 세트는 지우지 않고 deleted로 남긴다`
- [ ] 6. `삭제된 세트에 더 새로운 수정이 와도 되살리지 않는다`
- [ ] 7. `세션 종료 시각을 나중에 보내면 같은 세션에 채워진다`
- [ ] 8. `세션이 없는 세트는 SESSION_NOT_FOUND로 실패하고 나머지 항목은 저장한다`
- [ ] 9. `다른 사용자의 세션을 가리키는 세트는 SESSION_NOT_FOUND로 실패한다`
- [ ] 10. `같은 client_id라도 사용자가 다르면 따로 저장된다`
- [ ] 11. `모르는 종목의 세트는 UNKNOWN_EXERCISE로 실패한다`
- [ ] 12. `반복 수가 0이면 INVALID_VALUE로 실패한다`
- [ ] 13. `RPE가 0.5 단위가 아니면 INVALID_VALUE로 실패한다`
- [ ] 14. `종료가 시작보다 빠른 세션은 INVALID_VALUE로 실패한다`
- [ ] 15. `묶음이 100건을 넘으면 400 INVALID_INPUT으로 응답하고 아무것도 저장하지 않는다`
- [ ] 16. `빈 묶음이면 400 INVALID_INPUT으로 응답한다`
- [ ] 17. `수정 시각이 서버 시각보다 5분 넘게 미래면 서버 수신 시각으로 낮춰 저장한다`
- [ ] 18. `결과는 요청 순서대로 돌려준다`
- [ ] 19. `세션 시작 시각은 UTC 그대로 저장된다` (JdbcTemplate으로 원본 값 확인. CLAUDE.md 시간 값 규칙)
- [ ] 20. `JDBC로 저장한 client_id를 JPA로 읽으면 같은 UUID다` (바이트 순서)
- [ ] 21. `토큰 없이 보내면 401 UNAUTHORIZED로 응답한다`
- [ ] 22. `탈퇴한 회원이 보내면 403 FORBIDDEN으로 응답한다`

종목 조회
- [ ] 23. `활성 종목을 부위와 id 순으로 돌려준다` (`ExerciseControllerTest`, 통합. 시드 65개, 벤치 프레스 필드 확인)
- [ ] 24. `비활성 종목은 목록에 없다` (`ExerciseServiceTest`, `@RepositoryTest`로 한 종목을 비활성화하고 롤백)

동시성 (`WorkoutSyncConcurrencyTest`, 스레드 10개, 기존 동시성 테스트 패턴)
- [ ] 25. `같은 세트를 동시에 10번 보내도 행은 하나이고 모두 성공한다` (500 없음)
- [ ] 26. `같은 항목 20건을 서로 반대 순서로 담은 묶음 두 개를 동시에 보내도 중복 행 없이 모두 성공한다` (데드락 방지 순서 확인)

## 작업 순서

1. `feat/onboarding-server`에서 `feat/workout-sync-server` 브랜치 → 명세·계획 첫 커밋
2. 시그니처(DTO, 빈 서비스·저장소·검증기, 컨트롤러, `requireRecordable`, 종목 조회) → 테스트 1~26 → 실패 확인 → 보고
3. 구현: 저장소 SQL·UUID 변환 → 검증기 → 서비스 → 컨트롤러·종목 조회
4. 전체 `./gradlew test`
5. 성능 확인: 100건 묶음을 반복 보내 처리 시간을 잰다 (워크플로 "성능 확인" 단계, 결과는 계획 변경 이력에. 목표 p95 300ms)
6. `/review-change` → 반영 → 커밋. ai-log는 운동 기록(011+012)이 끝난 뒤 한 줄. push·PR은 사용자

## 확인 필요

- 없음 (명세 열린 질문은 모두 확정). 세트에 `server_received_at`을 추가하지 않는 것은 "데이터 변경"에 적었다.

## 변경 이력
- 2026-11-28: 계획대로 승인
- 2026-11-28 (테스트 작성): 테스트 23을 둘로 나눴다. 정렬만 확인하면 빈 목록에서도 통과해서, 비어 있지 않음을 함께 확인하고 앱이 쓰는 필드(이름·부위·부하 방식·기본 휴식 시간)를 확인하는 테스트를 하나 더했다. 테스트 26개 → 27개
- 2026-11-28 (구현 중, 사용자 승인): **회원별 Redis 락 추가.** 계획대로 "INSERT 시도 → 중복 키면 조건부 UPDATE"만 쓰니, 같은 세트를 동시에 10번 보내면 8건이 `Deadlock found when trying to get lock`으로 실패했다(임시 테스트로 예외 확인). 중복 키로 실패한 INSERT가 기존 행에 공유(S) 잠금을 남기고, 이어지는 UPDATE가 배타(X) 잠금을 기다리며 서로 막힌다. 한 행에서 생기므로 client_id 정렬로는 막을 수 없다. 계획의 "유니크 잠금이 줄 세운다"는 INSERT 단계만 본 판단이었다
  - 해결: Redisson 락 `workout-sync:{userId}`로 같은 회원의 동기화를 줄 세운다. 락은 트랜잭션 시작 전에 잡고 커밋 뒤에 푼다(`TransactionTemplate`). 커밋 전에 풀면 다음 요청이 커밋 안 된 행과 다시 부딪힌다. 다른 회원끼리는 기다리지 않는다. DB 유니크는 최후 안전망으로 그대로 둔다 (CLAUDE.md 동시성 원칙)
  - 락을 `fitness.workout.sync-lock-wait`(기본 5초, 테스트 1초) 안에 못 잡으면 503 `SYNC_BUSY`(ErrorCode 추가), 앱은 다시 보낸다. 락 시간을 정하지 않아 Redisson watchdog이 연장하고, 서버가 죽으면 30초 뒤 풀린다
  - 테스트 추가: `같은 회원의 다른 동기화가 락을 오래 잡고 있으면 SYNC_BUSY로 거절한다`. 테스트 27개 → 28개 (서버 전체 181개)
  - **왜 Redisson인가 (다른 방식과 비교).** 막아야 할 것은 "아직 없는 행을 같은 회원의 요청 여러 개가 동시에 넣을 때"의 경쟁이다
    - 낙관적 락(`@Version`): 이미 있는 행을 두 요청이 고칠 때의 덮어쓰기 유실을 잡는 방식이라, 비교할 버전이 없는 INSERT 경쟁에는 맞지 않는다. 덮어쓰기 유실은 조건부 UPDATE(`client_updated_at < 새 값`)가 이미 막는다
    - 비관적 락(세트 행에 `SELECT … FOR UPDATE`): 없는 행에 걸면 REPEATABLE READ에서 빈 구간(gap) 잠금이 걸리고, 여러 요청이 같은 구간을 잡은 채 서로의 INSERT를 기다리는 또 다른 데드락 패턴이 된다
    - 비관적 락(회원 행 `users … FOR UPDATE`, 009 방식): 동작은 하지만 user 모듈 테이블을 workout이 잠가 모듈 경계를 넘고, 동기화 동안 프로필 수정 같은 다른 회원 작업까지 막는다
    - MySQL 이름 락(`GET_LOCK`): DB만으로 회원별로 줄 세울 수 있어 대안이 된다. 다만 락이 DB 커넥션에 묶여, 커넥션 풀과 Spring 트랜잭션 아래에서 "트랜잭션 전에 잡고 커밋 뒤 같은 커넥션으로 푼다"를 직접 맞춰야 한다
    - 데드락 시 재시도: 결과는 맞지만(멱등) 데드락 자체는 계속 나고, 몰리면 되돌림·재시도가 반복된다
    - Redisson을 고른 이유: Redis와 `redisson-spring-boot-starter`가 이미 있고(refresh 토큰, ADR 0004), 락이 DB 커넥션과 분리돼 `TransactionTemplate` 앞뒤로 감싸기만 하면 되며, `tryLock` 대기 한도와 watchdog(서버가 죽으면 30초 뒤 해제)을 그대로 쓸 수 있다. 서버를 여러 대로 늘려도 동작하고, CLAUDE.md 동시성 원칙(Redis + DB 유니크 최후 안전망)과 같은 모양이라 챌린지 선착순과 패턴을 맞출 수 있다
    - 감수한 점: Redis 장애 때 동기화가 실패한다(앱이 기기 기록을 다시 보내 유실은 없음, 로그인 갱신과 같은 장애 범위). 장애 조치 중 락이 잠깐 겹칠 수 있어(Redlock 논쟁) DB 유니크를 최후 안전망으로 남겼고, 그때 최악은 중복 행이 아니라 데드락 500 한 번이다. 락 왕복 1~2ms는 성능 측정(p95 22ms)에 포함돼 있다
- 2026-11-28 (구현 중, 사용자 승인): 종목 필드 테스트가 응답 본문을 `contentAsString`(charset 없는 응답은 ISO-8859-1로 읽음)으로 읽어 한글이 깨졌다 → `getContentAsString(UTF_8)`로 읽는다. 기대값은 그대로
- 2026-11-28 (성능 확인): 로컬(MySQL·Redis 로컬, MockMvc로 HTTP 계층 포함)에서 세션 1 + 세트 99 = 100건 묶음을 30번 보냄. 새로 넣기 p50 13ms·p95 22ms·최대 32ms, 같은 묶음 재전송(모두 IGNORED) p50 12ms·p95 17ms·최대 39ms. 목표 p95 300ms 안. 운영 환경(네트워크 왕복)에서는 다시 잰다. 묶음 저장(멀티 로우 INSERT) 최적화는 지금 필요 없음
