# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Traffic-Gym (나만의 트레이너 쌤)

내 신체 정보와 운동 기록을 아는 AI 트레이너가 매일 오늘의 운동·식단을 짜서 알려주는 앱의 백엔드. 요구사항은 `docs/prd.md`(v2)가 기준이다.

> **전환 중:** PRD v2로 요구사항이 바뀌었다. 아래 "패키지 구조"와 `docs/frd.md`, `docs/erd-dia.md`, 기존 엔티티(trainer, ptclass, reservation)는 이전 버전(PT 클래스 수강신청) 기준이다. 새 도메인은 PRD 10장의 모듈(user, workout, routine, notification, challenge, llm)을 따르고, 구조가 확정되면 이 문서를 갱신한다.

## 아키텍처 원칙
- 숫자(무게·세트·반복 수·칼로리)는 서버 규칙이 계산한다. LLM은 종목 구성·순서·문구만 만든다.
- LLM에는 서버가 SQL로 거른 후보 종목만 넘긴다. LLM 응답은 JSON 스키마·규칙 검증을 통과해야 저장하고, 실패하면 대체 응답(기본 루틴)을 쓴다.
- 멱등성이 필요한 쓰기는 `(user_id, client_id)` 유니크 제약 + upsert로 처리한다.
- 동시성이 필요한 쓰기(챌린지 정원 등)는 `docs/decisions/`의 결정을 따른다. Redis 원자 연산 + DB 유니크 제약을 최후 안전망으로 둔다.
- 서버 메모리에 상태를 두지 않는다 (여러 대로 늘려도 동작해야 한다).

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
- MySQL 8.x (JPA + QueryDSL), Redis (Spring Data Redis + Redisson), Kafka
- Actuator + Micrometer(Prometheus), springdoc-openapi(Swagger)
- 테스트: JUnit5, Mockk

## 개발 명령어

```bash
./gradlew build                 # 전체 빌드 (컴파일 + 테스트)
./gradlew bootRun               # 로컬 실행 (local 프로파일, 8080 포트)
./gradlew test                  # 전체 테스트
./gradlew test --tests "com.fitness.reservation.ReservationServiceTest"              # 클래스 단위
./gradlew test --tests "com.fitness.reservation.ReservationServiceTest.잔여 수량이 0이면*"  # 메서드 단위 (백틱 테스트명은 글롭 패턴으로 매칭)
```

- Gradle 래퍼는 **8.10 고정**이다. Kotlin 1.9.25의 kapt가 Gradle 9의 API 변경과 호환되지 않아 임의로 올리면 빌드가 깨진다.
- QueryDSL Q클래스는 kapt가 `build/generated/source/kapt/main`에 생성한다. 엔티티 필드를 바꾼 뒤 Q클래스가 안 잡히면 `./gradlew clean compileKotlin`으로 재생성한다.
- formatter는 프로젝트에 없고, ktlint가 로컬에 설치돼 있으면 훅이 수정한 파일에만 적용한다.

### 자동 검증 훅 (`.claude/settings.json`)
- Kotlin 파일 수정 직후: ktlint 포맷(설치 시) + `compileKotlin compileTestKotlin`. 실패하면 에러가 돌아오니 바로 고친다.
- 작업 종료 시: 코드 변경이 있으면 `./gradlew test`. 실패하면 최대 3번까지 스스로 고친다.
- 훅이 잡은 오류는 `.claude/hooks/caught.log`에 쌓인다 (`docs/ai-log.md` 기록용).

## 패키지 구조

**도메인별 패키지, 도메인 내부는 레이어별**로 구성한다. (`com.fitness.<domain>.<layer>`)

```
com.fitness
├── FitnessApplication.kt
├── config/              # 전역 설정 빈 (Swagger, Redis, Security, Kafka, QueryDSL 등)
├── common/              # 도메인에 속하지 않는 공통 코드
│   ├── entity/          # BaseTimeEntity 등 공통 엔티티 베이스
│   ├── exception/       # GlobalExceptionHandler, 커스텀 예외, 에러 코드
│   ├── response/        # 공통 API 응답 포맷
│   └── util/
├── auth/                # 소셜 로그인, JWT 발급/재발급
│   ├── controller/
│   ├── service/
│   ├── client/          # 카카오/구글 OAuth 외부 API 클라이언트
│   └── dto/
├── user/                # 유저 계정, 프로필, 역할 전환
│   ├── controller/
│   ├── service/
│   ├── domain/          # JPA 엔티티
│   ├── repository/
│   └── dto/
├── trainer/             # 트레이너 셀프 세일즈 프로필
│   ├── controller/
│   ├── service/
│   ├── domain/
│   ├── repository/
│   └── dto/
├── ptclass/             # PT 클래스 개설, 상세 조회(Cache-Aside), 캐시 프리워밍
│   ├── controller/
│   ├── service/
│   ├── domain/
│   ├── repository/
│   └── dto/
├── reservation/         # 선착순 수강신청 (Redis Lua 원자적 처리)
│   ├── controller/
│   ├── service/
│   ├── domain/
│   ├── repository/
│   ├── lock/            # Redis Lua script 실행기, Redisson 락
│   └── dto/
└── diet/                # 식단 기록 및 LLM 피드백
    ├── controller/
    ├── service/
    ├── domain/
    ├── repository/
    └── dto/
```

각 엔티티는 `com.fitness.common.entity.BaseTimeEntity`(`createdAt` JPA Auditing)를 상속한다.

새 도메인이 FRD에 추가되면 위와 같은 레이어 구조로 최상위에 패키지를 하나 더 만든다 (예: `payment/`, `notification/`).

### 도메인 간 참조 규칙
- 다른 도메인의 **Repository를 직접 주입받지 않는다.** 반드시 그 도메인의 Service를 통해서만 접근한다 (캡슐화 유지).
- 다른 도메인의 **JPA 엔티티를 연관관계로 직접 참조하는 것은 허용**하되(같은 모놀리스 DB이므로), 연관관계 편의 메서드나 비즈니스 로직은 엔티티가 속한 도메인 안에만 둔다.
- `common`, `config`는 도메인 코드에 의존하지 않는다 (역방향 의존 금지).

## 테스트 작성 규칙

모든 테스트는 **Given-When-Then**을 지켜서 작성한다.

- 테스트 메서드 본문을 `// given`, `// when`, `// then` 주석으로 3구간 구분한다. 각 구간은 빈 줄로 분리한다.
- `given`: 테스트 대상과 협력 객체 준비, Mockk 스텁 설정.
- `when`: 검증하려는 동작 단 한 번 호출. 여러 동작을 한 when에 넣지 않는다.
- `then`: 결과 검증(assertion) 및 필요한 경우 `verify`(Mockk) 호출.
- 테스트 함수명은 백틱을 사용한 한글 문장으로, "어떤 상황에서 무엇을 한다"가 드러나게 짓는다.
  예: `` fun `잔여 수량이 0이면 예약은 SLOT_EXHAUSTED 예외를 던진다`() ``
- 하나의 테스트는 하나의 시나리오만 검증한다 (then에서 서로 다른 관심사를 여러 개 assert하지 않는다).
- Mock은 Mockk(`mockk()`, `every`, `verify`)를 사용하고, `spring-boot-starter-test`의 Mockito는 쓰지 않는다.

```kotlin
@Test
fun `잔여 수량이 0이면 예약은 SLOT_EXHAUSTED 예외를 던진다`() {
    // given
    val classId = 1L
    every { reservationRepository.getRemainingCapacity(classId) } returns 0

    // when & then
    assertThrows<SlotExhaustedException> {
        reservationService.reserve(classId, userId = 1L)
    }
}
```

## Git 커밋 규칙
- 커밋 메시지는 **한글로 작성**한다.

## 로컬 실행 환경
- 인프라: MySQL(`fitness` DB), Redis — 둘 다 로컬에 직접 설치되어 있음 (Docker 미사용). 접속 정보(계정/비밀번호)는 `application-local.yml`(gitignore 대상, 커밋 금지)에 각자 설정.
- Redis는 `brew services`가 이 macOS에서 프로세스를 못 띄우는 이슈가 있어 `redis-server /opt/homebrew/etc/redis.conf --daemonize yes`로 수동 기동한다.
- 프로파일: 기본 활성 프로파일은 `local` (`application-local.yml`에 datasource/redis/kafka 접속 정보).
- `gradle.properties`가 아니라 `~/.gradle/gradle.properties`(전역, 미커밋)의 `org.gradle.java.home`이 Homebrew JDK 21을 가리킨다 — 로컬 JDK가 25뿐이라 Gradle 8.10 데몬 구동용으로 별도 지정한 것.