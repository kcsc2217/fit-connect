# Fit Connect (Traffic-Gym)

PT 클래스 선착순 수강신청 & 피트니스 플랫폼 백엔드.

트레이너가 셀프 세일즈 프로필과 PT 클래스를 개설하면, 유저가 정각 오픈 시 몰리는 트래픽 속에서 선착순으로 수강신청하는 서비스입니다. 상세 기능 명세는 [`docs/frd.md`](docs/frd.md) 참고.

## 기술 스택

| 구분 | 스택 |
| --- | --- |
| 언어 / 런타임 | Kotlin 1.9.25, JDK 21 |
| 프레임워크 | Spring Boot 3.3.3 |
| 빌드 도구 | Gradle 8.10 (Kotlin DSL) |
| 데이터베이스 | MySQL 8.x (Spring Data JPA + QueryDSL) |
| 캐시 / 분산 락 | Redis (Spring Data Redis, Redisson) |
| 메시징 | Apache Kafka |
| API 문서 | springdoc-openapi (Swagger UI) |
| 관측 | Actuator, Micrometer(Prometheus) |
| 테스트 | JUnit5, Mockk |

## 패키지 구조

도메인별 패키지, 도메인 내부는 레이어별로 구성합니다. 자세한 규칙과 도메인 간 참조 정책은 [`CLAUDE.md`](CLAUDE.md)를 참고하세요.

```
com.fitness
├── config/          # 전역 설정 (Swagger, Redis, Security, Kafka 등)
├── common/          # 도메인 독립적인 공통 코드
├── auth/            # 소셜 로그인, JWT
├── user/            # 유저 계정 / 프로필
├── trainer/         # 트레이너 셀프 세일즈 프로필
├── ptclass/         # PT 클래스 개설 / 조회 / 캐시 프리워밍
└── reservation/     # 선착순 수강신청 (Redis Lua 원자적 처리)
```

## 로컬 실행

### 사전 준비
- JDK 21
- MySQL 8.x (로컬에 `fitness` 데이터베이스 필요)
- Redis

```sql
CREATE DATABASE fitness CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 환경 설정
`src/main/resources/application-local.yml`을 만들어 본인 로컬 DB/Redis 접속 정보를 채웁니다 (git에는 포함되지 않는 개인 설정 파일입니다).

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fitness?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: root
    password: <your-local-mysql-password>
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: create
    show-sql: true
  data:
    redis:
      host: localhost
      port: 6379
  kafka:
    bootstrap-servers: localhost:9092
```

### 실행

```bash
./gradlew bootRun
```

- 앱: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- 헬스체크: `http://localhost:8080/actuator/health`

### 빌드 / 테스트

```bash
./gradlew build
./gradlew test
```