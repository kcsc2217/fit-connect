# 009 온보딩 서버 API 구현 계획

- 명세: docs/specs/onboarding.md (확정 2026-11-15)
- 크기: L의 일부 (온보딩 중 1/2. 다음은 010 앱 온보딩 화면)
- 상태: 승인됨 (2026-11-15)
- 브랜치: `feat/onboarding-server`

## Context

로그인한 신규 회원은 상태가 ONBOARDING인데, 프로필을 저장하고 ACTIVE로 바꿀 API가 없다. 그래서 앱은 "온보딩 준비 중" 자리 화면과 건너뛰기만 갖고 있다. 이번 작업에서 프로필·장비를 저장하고 상태를 바꾸는 API와 조회 API를 만든다. 테이블·엔티티는 V1(계획 001)에 이미 있으므로 스키마는 바꾸지 않는다. 오늘의 운동(경력·목표·분할·장비)과 알림(시각·시간대·요일)이 이 값을 읽어 간다.

## 접근

- **API 하나로 생성·수정을 함께 한다 (`PUT /api/v1/users/me/profile`).** 프로필은 사용자당 하나이고 PK가 곧 user_id라, "없으면 만들고 있으면 덮어쓰기"가 자연스럽다. 같은 요청을 다시 보내도 결과가 같다(멱등). FR-04 설정 화면도 이 API를 그대로 쓴다.
- **같은 회원의 제출은 `users` 행 잠금으로 줄 세운다.** 트랜잭션 시작에서 `SELECT ... FOR UPDATE`로 회원 행을 잡는다. 회원 행은 로그인 때 이미 있으므로 항상 잠글 대상이 있다. 동시에 처음 제출해도 둘째 요청은 첫째가 커밋된 뒤 프로필이 "있는" 상태에서 시작해서, 둘 다 INSERT하다 PK가 충돌하는 일이 없다. 잠금 범위는 그 회원 한 명이라 다른 회원과는 겹치지 않고, 같은 회원이 동시에 여러 번 제출하는 일은 드물다(버튼 연타, 재전송). 그래서 비용이 거의 없다.
- **장비는 "바뀐 것만" 지우고 넣는다.** 지금 장비와 요청 장비를 비교해 빠진 것만 지우고 새것만 넣는다. 전부 지운 뒤 다시 넣으면, Hibernate가 flush할 때 INSERT를 DELETE보다 먼저 실행해서 같은 PK가 충돌하기 때문이다.
- **요일은 API에서 `DayOfWeek` 목록(`["MONDAY","WEDNESDAY"]`)으로 주고받는다.** 비트마스크 변환은 서버 `WorkoutDays` 한곳에서만 한다. 명세 예시의 `MON`처럼 새 enum을 만들지 않고 Java 표준 `java.time.DayOfWeek`를 쓴다.
- **검증은 두 곳에서 한다.** 고정 범위(키·몸무게·요일 1개 이상)는 DTO의 Bean Validation이 맡는다. 현재 시각이나 해석이 필요한 값(출생 연도 = 올해 기준, 시간대, 알림 5분 단위, 운동 시간 선택지)은 서비스가 맡는다. 서비스 검증 실패도 기존 `INVALID_INPUT` + `errors[]` 형식으로 응답하도록 `InvalidInputException(field, message)`를 추가한다.

## 변경 파일

| 파일 | 변경 | 내용 |
| --- | --- | --- |
| user/controller/UserProfileController.kt | 신규 | `PUT`·`GET /api/v1/users/me/profile`, `@AuthenticationPrincipal userId: Long`, Swagger 문서 |
| user/service/UserProfileService.kt | 신규 | 제출(검증 → 잠금 → 프로필 upsert → 장비 diff → 상태 전환), 조회 |
| user/dto/ProfileDtos.kt | 신규 | `ProfileRequest`(Bean Validation), `ProfileResponse`(`status` 포함). 엔티티를 직접 반환하지 않음 |
| user/domain/WorkoutDays.kt | 신규 | `DayOfWeek` 목록 ↔ 비트마스크 (월=1 … 일=64) |
| user/domain/User.kt | 수정 | `completeOnboarding()`: ONBOARDING → ACTIVE, ACTIVE는 그대로, WITHDRAWN이면 `BusinessException(FORBIDDEN)` |
| user/domain/UserProfile.kt | 수정 | `update(...)`: 모든 필드를 한 번에 덮어쓰는 메서드 (필드별 setter 대신) |
| user/repository/UserRepository.kt | 수정 | `findByIdForUpdate(id)`: `@Lock(PESSIMISTIC_WRITE)` |
| common/exception/InvalidInputException.kt | 신규 | `BusinessException(INVALID_INPUT)` 상속, `field`·`message`를 가짐 |
| common/exception/GlobalExceptionHandler.kt | 수정 | `InvalidInputException`을 `invalidInput(errors)` 형식으로 응답 (기존 `@Valid` 실패와 같은 모양) |
| test/.../user/controller/UserProfileControllerTest.kt | 신규 | 통합 테스트 (아래 목록) |
| test/.../user/service/UserProfileConcurrencyTest.kt | 신규 | 동시 제출 |
| test/.../user/domain/WorkoutDaysTest.kt, UserTest.kt | 신규 | 단위 테스트 |
| test/.../support/ProfileTestSupport.kt | 신규 | 유효한 요청 JSON을 만드는 도우미 (필드 하나씩 바꿔 끼우기) |
| CLAUDE.md | 수정 | 진행 상황 한 줄, 패키지 구조 주석("온보딩 프로필 구현됨"), `InvalidInputException` 사용법 한 줄 |

## 데이터 변경

없음. 기존 `user_profile`(PK user_id, 인덱스 alarm_time), `user_equipment`(PK (user_id, equipment)), `users.status`를 그대로 쓴다. 마이그레이션도 없다.

## API

| 메서드 | 경로 | 요청 | 응답 | 에러 |
| --- | --- | --- | --- | --- |
| PUT | /api/v1/users/me/profile | `ProfileRequest` | 200 `ProfileResponse` | 400 `INVALID_INPUT`(errors[]), 401, 403 `FORBIDDEN`(탈퇴 회원), 404 `NOT_FOUND`(회원 없음) |
| GET | /api/v1/users/me/profile | - | 200 `ProfileResponse` | 401, 404 `NOT_FOUND`(온보딩 전 또는 회원 없음) |

```json
// ProfileRequest
{
  "heightCm": 175.0, "weightKg": 72.5, "birthYear": 1997, "sex": "MALE",
  "goal": "BULK", "experience": "INTERMEDIATE",
  "equipment": ["BARBELL", "DUMBBELL"],
  "workoutDays": ["MONDAY", "WEDNESDAY", "FRIDAY"],
  "splitType": "THREE_SPLIT", "alarmTime": "19:00", "timezone": "Asia/Seoul", "sessionMinutes": 60
}
// ProfileResponse = 위 필드 + "status": "ACTIVE"
// equipment는 enum 선언 순서, workoutDays는 월→일 순서로 정렬해서 돌려준다
```

- `alarmTime`은 `@JsonFormat(pattern = "HH:mm")`으로 요청·응답 모두 `"19:00"`이다.
- 장비 중복(`["BARBELL","BARBELL"]`)은 하나로 저장한다 (에러 아님).

## 핵심 코드 스케치

### 제출 (UserProfileService)

```kotlin
@Transactional
fun submit(userId: Long, request: ProfileRequest): ProfileResponse {
    validate(request)                                       // 출생 연도, 시간대, 5분 단위, 운동 시간 → InvalidInputException

    // 같은 회원의 동시 제출을 줄 세운다. 회원 행은 로그인 때 만들어져 항상 있다
    val user = userRepository.findByIdForUpdate(userId)
        ?: throw BusinessException(ErrorCode.NOT_FOUND, "회원을 찾을 수 없습니다")
    user.completeOnboarding()                               // WITHDRAWN이면 FORBIDDEN

    val workoutDays = WorkoutDays.toMask(request.workoutDays)
    val profile = userProfileRepository.findByIdOrNull(userId)
        ?.apply { update(request.heightCm, ..., workoutDays, ...) }
        ?: userProfileRepository.save(request.toEntity(userId, workoutDays))

    replaceEquipment(userId, request.equipment.toSet())
    return ProfileResponse.of(profile, currentEquipment, user.status)
}

// 전부 지우고 다시 넣으면 flush 순서(INSERT가 DELETE보다 먼저) 때문에 같은 PK가 충돌한다. 차이만 반영한다
private fun replaceEquipment(userId: Long, wanted: Set<Equipment>) {
    val current = userEquipmentRepository.findAllByIdUserId(userId)
    userEquipmentRepository.deleteAll(current.filter { it.id.equipment !in wanted })
    val have = current.map { it.id.equipment }.toSet()
    userEquipmentRepository.saveAll((wanted - have).map { UserEquipment(UserEquipmentId(userId, it)) })
}

private fun validate(r: ProfileRequest) {
    val zone = runCatching { ZoneId.of(r.timezone) }.getOrNull()
        ?: throw InvalidInputException("timezone", "올바른 시간대가 아닙니다")
    val thisYear = LocalDate.now(clock.withZone(zone)).year // 사용자 시간대의 올해 (ADR 0002)
    if (r.birthYear !in (thisYear - MAX_AGE)..(thisYear - MIN_AGE)) throw InvalidInputException("birthYear", "...")
    if (r.alarmTime.minute % 5 != 0 || r.alarmTime.second != 0) throw InvalidInputException("alarmTime", "5분 단위로 입력해 주세요")
    if (r.sessionMinutes !in SESSION_MINUTES) throw InvalidInputException("sessionMinutes", "...") // 30, 45, 60, 90
}
```

- `UserEquipment`는 ID를 직접 정하는 엔티티라 새 객체를 `save`하면 merge되고, 반환된 객체의 `createdAt`이 null이다 (CLAUDE.md). 그래서 응답의 장비 목록은 엔티티의 시각을 쓰지 않고 `wanted` 집합을 정렬해서 만든다.
- `UserProfile`도 ID를 직접 정한다. 응답에는 시각 필드를 넣지 않으므로 같은 문제가 없다.

### 잠금 (UserRepository)

```kotlin
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select u from User u where u.id = :id")
fun findByIdForUpdate(id: Long): User?
```

### 요일 변환 (WorkoutDays)

```kotlin
object WorkoutDays {
    // DayOfWeek.value: 월=1 … 일=7 → 비트 1, 2, 4 … 64 (db-schema user_profile)
    fun toMask(days: Collection<DayOfWeek>): Int = days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }
    fun fromMask(mask: Int): List<DayOfWeek> = DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }
}
```

### 상태 전환 (User)

```kotlin
fun completeOnboarding() {
    when (status) {
        UserStatus.ONBOARDING -> status = UserStatus.ACTIVE
        UserStatus.ACTIVE -> Unit                            // 다시 제출 = 수정
        UserStatus.WITHDRAWN -> throw BusinessException(ErrorCode.FORBIDDEN, "탈퇴한 회원입니다")
    }
}
```

### DTO 검증 (고정 범위만)

```kotlin
data class ProfileRequest(
    @field:DecimalMin("100.0") @field:DecimalMax("250.0") @field:Digits(integer = 3, fraction = 1)
    val heightCm: BigDecimal,
    @field:DecimalMin("30.0") @field:DecimalMax("300.0") @field:Digits(integer = 3, fraction = 1)
    val weightKg: BigDecimal,
    val birthYear: Short,
    val sex: Sex, val goal: Goal, val experience: Experience,
    val equipment: List<Equipment>,                          // 빈 목록 허용 (맨몸 종목만 후보)
    @field:NotEmpty val workoutDays: List<DayOfWeek>,
    val splitType: SplitType,
    @JsonFormat(pattern = "HH:mm") val alarmTime: LocalTime,
    val timezone: String,
    val sessionMinutes: Short,
)
```

- 모르는 enum 값이나 빠진 필드는 Jackson이 `@Valid`보다 먼저 실패한다. 기존 `handleHttpMessageNotReadable`이 이 경우를 `INVALID_INPUT` + 필드 경로로 바꿔 준다 (계획 005).
- 소수 둘째 자리(`175.25`)는 `@Digits(fraction = 1)`가 400으로 막는다. DB가 조용히 반올림해 저장하지 않게 하려는 것이다.

## 정합성·동시성·멱등성

- **부분 저장:** 프로필, 장비, 상태가 하나의 `@Transactional` 안에 있다. 중간에 실패하면 모두 롤백되어 "프로필은 있는데 ONBOARDING" 같은 상태가 생기지 않는다.
- **동시 첫 제출:** 잠금이 없으면 두 요청이 모두 "프로필 없음"을 보고 INSERT해서 둘째가 PK 충돌로 500이 된다. `users` 행 `FOR UPDATE`로 막는다. 둘째 요청은 첫째의 커밋을 기다렸다가 UPDATE 경로로 간다. 최종 값은 나중에 끝난 요청의 값이다. 동시성 테스트로 확인한다.
- **장비 PK 충돌:** 위의 diff 방식으로 같은 PK의 DELETE와 INSERT를 한 flush에 섞지 않는다.
- **멱등:** 같은 요청을 다시 보내면 같은 값으로 덮어쓰고, 상태는 이미 ACTIVE라 그대로다. `client_id`는 필요 없다.
- **서버 메모리 상태:** 없다. 잠금은 DB에 있다.
- **잠금 대기:** MySQL InnoDB 기본 `innodb_lock_wait_timeout`(50초) 안에서 기다린다. 같은 회원의 짧은 트랜잭션끼리만 기다리므로 실제 대기는 수 ms다.

## 실패·대체 경로

외부 호출 없음. DB 실패는 기존 `GlobalExceptionHandler`가 500 `INTERNAL_ERROR`로 응답하고 트랜잭션은 롤백된다. 로그에는 신체 값을 남기지 않는다 (요청 본문을 로깅하지 않음).

## 테스트 목록

통합 (`UserProfileControllerTest`, `@IntegrationTest`, `loginAsDev`로 테스트마다 새 회원)
- [ ] 1. `온보딩 회원이 프로필을 제출하면 상태가 ACTIVE인 프로필을 돌려준다`
- [ ] 2. `프로필을 제출하면 내 정보 조회의 상태도 ACTIVE가 된다`
- [ ] 3. `제출한 프로필을 조회하면 요일은 월요일부터, 알림 시각은 시:분으로 돌려준다`
- [ ] 4. `같은 프로필을 두 번 제출해도 같은 결과를 돌려준다`
- [ ] 5. `ACTIVE 회원이 다시 제출하면 빠진 장비는 지우고 새 장비는 추가한다`
- [ ] 6. `장비를 빈 목록으로 제출하면 장비가 모두 지워진다`
- [ ] 7. `운동 요일이 비어 있으면 400 INVALID_INPUT과 workoutDays 필드 오류로 응답한다`
- [ ] 8. `키가 250cm를 넘으면 400 INVALID_INPUT과 heightCm 필드 오류로 응답한다`
- [ ] 9. `몸무게가 소수 둘째 자리까지 있으면 400 INVALID_INPUT으로 응답한다`
- [ ] 10. `나이가 14세 미만인 출생 연도면 400 INVALID_INPUT과 birthYear 필드 오류로 응답한다`
- [ ] 11. `알림 시각이 5분 단위가 아니면 400 INVALID_INPUT과 alarmTime 필드 오류로 응답한다`
- [ ] 12. `운동 시간이 선택지에 없으면 400 INVALID_INPUT과 sessionMinutes 필드 오류로 응답한다`
- [ ] 13. `시간대 이름이 잘못되면 400 INVALID_INPUT과 timezone 필드 오류로 응답한다`
- [ ] 14. `모르는 목표 값을 보내면 400 INVALID_INPUT과 goal 필드 오류로 응답한다`
- [ ] 15. `검증에 실패하면 회원 상태는 ONBOARDING 그대로다`
- [ ] 16. `토큰 없이 제출하면 401 UNAUTHORIZED로 응답한다`
- [ ] 17. `탈퇴한 회원이 제출하면 403 FORBIDDEN으로 응답한다` (회원을 저장소로 만들고 `withdraw`, 토큰은 `JwtProvider`로 발급)
- [ ] 18. `토큰은 유효한데 회원이 없으면 제출은 404 NOT_FOUND로 응답한다`
- [ ] 19. `온보딩 전에 프로필을 조회하면 404 NOT_FOUND로 응답한다`
- [ ] 20. `운동 요일은 비트마스크로 저장된다` (JdbcTemplate으로 `workout_days` 원본 값 확인: 월·수·금 → 21)

동시성 (`UserProfileConcurrencyTest`, `@IntegrationTest`, 스레드 10개, 기존 `AuthServiceConcurrencyTest` 패턴)
- [ ] 21. `같은 회원이 처음 제출을 동시에 10번 보내도 모두 성공하고 프로필은 하나다`
- [ ] 22. `같은 회원이 장비가 다른 제출을 동시에 보내도 장비는 어느 한 요청의 목록과 같다` (섞이지 않음)

단위
- [ ] 23. `월·수·금은 비트마스크 21이 되고 21은 다시 월·수·금이 된다` (`WorkoutDaysTest`)
- [ ] 24. `일요일만 고르면 비트마스크 64가 된다` (경계)
- [ ] 25. `ONBOARDING 회원이 온보딩을 마치면 ACTIVE가 된다` (`UserTest`)
- [ ] 26. `탈퇴한 회원이 온보딩을 마치려 하면 FORBIDDEN 예외를 던진다`

## 작업 순서

1. `main` 최신화 → `feat/onboarding-server` 브랜치. 명세(`docs/specs/onboarding.md`)와 이 계획(`docs/plans/009-onboarding-server.md`)을 첫 커밋으로
2. 시그니처만 만들고(DTO, 빈 서비스·컨트롤러, `WorkoutDays`, `completeOnboarding`, `InvalidInputException`) 테스트 1~26 작성 → 실패 확인 → 보고
3. `/new-api`로 구현: `WorkoutDays`·`User` → `InvalidInputException`·핸들러 → 서비스 → 컨트롤러
4. 전체 `./gradlew test`, Swagger에서 요청 모양 확인
5. `/review-change` → 반영 → ai-log 기록 → 커밋. push·PR은 사용자

## 확인 필요

- (해결) 요일 값 이름: Java 표준 `DayOfWeek`(`MONDAY`)를 쓴다. 명세 예시를 함께 고쳤다.

## 변경 이력
- 2026-11-15: 계획대로 승인. "확인 필요"의 요일 값 이름은 `DayOfWeek`(`MONDAY`)로 확정하고 명세 예시(`MON`)를 고쳤다
- 2026-11-15 (구현 중): 테스트 3·5의 목록 단언을 `value(listOf(...))`에서 Hamcrest `value(contains(...))`로 바꿨다. Spring의 `jsonPath().value()`는 기대값의 클래스로 JSON을 변환하는데, Kotlin `listOf`의 구현 클래스(`Arrays$ArrayList`)로는 변환하지 못해 실제 값이 `null`로 읽혔다 (같은 필드의 `isEmpty()`는 통과). 기대값과 순서 검증은 그대로다
- 2026-11-15 (리뷰 반영, 지적 6건 모두): 테스트 26개 → 34개
  1. (중간) 요일·장비 목록의 null 원소(`["MONDAY", null]`)가 500이던 것 → DTO 원소를 nullable(`List<DayOfWeek?>`, `List<Equipment?>`)로 받고 서비스가 `InvalidInputException`으로 400. 원소 `@NotNull`은 Kotlin이 타입 어노테이션을 바이트코드에 쓰지 않아(기본 설정) 동작하지 않는다. 테스트 2개 추가
  2. Short 범위를 넘는 숫자(`birthYear: 70000`)가 필드 없는 `INVALID_REQUEST`이던 것 → `handleHttpMessageNotReadable`이 `MismatchedInputException`뿐 아니라 경로가 있는 `JsonMappingException` 전체를 `INVALID_INPUT` + 필드로 응답. 테스트 1개 추가
  3. 정수 필드의 소수(`sessionMinutes: 60.9`)가 잘려 받아지던 것 → 전역 `spring.jackson.deserialization.accept-float-as-int: false`. 테스트 1개 추가, 전체 테스트로 다른 API 영향 없음 확인
  4. 테스트 5·6·15의 given 제출 결과를 확인하지 않던 것 → 상태 단언 추가
  5. 경계값 테스트 4개 추가 (올해 14세·100세 통과, 101세 거절, 키 250.0 통과)
  6. 동시 첫 제출이 안전한 조건(잠금 앞에 일반 조회를 두지 않음, REPEATABLE READ 스냅샷)을 서비스 주석에 남김
  - 지적 1~3은 임시 테스트로 실제 요청을 보내 재현한 뒤 고쳤다
