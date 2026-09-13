# Traffic-Gym (Fitness Platform)

PT 클래스 선착순 수강신청 & 피트니스 플랫폼. 상세 기능 명세는 `docs/frd.md` 참고.

## 기술 스택
- Kotlin 1.9.25 / JDK 21 / Spring Boot 3.3.3 / Gradle Kotlin DSL
- MySQL 8.x (JPA + QueryDSL), Redis (Spring Data Redis + Redisson), Kafka
- Actuator + Micrometer(Prometheus), springdoc-openapi(Swagger)
- 테스트: JUnit5, Mockk

## 패키지 구조

**도메인별 패키지, 도메인 내부는 레이어별**로 구성한다. (`com.fitness.<domain>.<layer>`)

```
com.fitness
├── FitnessApplication.kt
├── config/              # 전역 설정 빈 (Swagger, Redis, Security, Kafka, QueryDSL 등)
├── common/              # 도메인에 속하지 않는 공통 코드
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
└── reservation/         # 선착순 수강신청 (Redis Lua 원자적 처리)
    ├── controller/
    ├── service/
    ├── domain/
    ├── repository/
    ├── lock/            # Redis Lua script 실행기, Redisson 락
    └── dto/
```

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

## 로컬 실행
- 인프라: MySQL(`fitness` DB), Redis — 둘 다 로컬에 직접 설치되어 있음 (Docker 미사용). 접속 정보(계정/비밀번호)는 `application-local.yml`(gitignore 대상, 커밋 금지)에 각자 설정. Redis는 `brew services`가 이 macOS에서 프로세스를 못 띄우는 이슈가 있어 `redis-server /opt/homebrew/etc/redis.conf --daemonize yes`로 수동 기동한다.
- 프로파일: 기본 활성 프로파일은 `local` (`application-local.yml`에 datasource/redis/kafka 접속 정보).
- 실행: `./gradlew bootRun`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Gradle 래퍼는 8.10 고정 (Kotlin 1.9.25의 kapt가 Gradle 9 API 변경과 호환되지 않음). `gradle.properties`의 `org.gradle.java.home`이 Homebrew JDK 21을 가리킨다.