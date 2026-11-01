# 나만의 트레이너 쌤 (Traffic-Gym)

내 신체 정보와 운동 기록을 아는 AI 트레이너가, 내가 정한 시각에 오늘의 운동과 식단을 짜서 알려주는 모바일 앱의 백엔드입니다.

## 문서

| 문서 | 내용 |
| --- | --- |
| [docs/prd.md](docs/prd.md) | 요구사항 (PRD v2) |
| [docs/db-schema.md](docs/db-schema.md) | DB 스키마 (1차 오픈) |
| [docs/architecture.md](docs/architecture.md) | 아키텍처 초안 |
| [docs/workflow.md](docs/workflow.md) | 개발 작업 절차 (AI 개발 체계) |
| [CLAUDE.md](CLAUDE.md) | 코드 규칙, 개발 명령어, 로컬 환경 |

## 기술 스택

Kotlin 1.9.25, JDK 21, Spring Boot 3.3.3, Gradle 8.10 (Kotlin DSL), MySQL 8, Redis, JUnit5 + Mockk

## 로컬 실행

1. MySQL 8과 Redis를 로컬에 설치합니다.
2. `src/main/resources/application-local.yml`에 본인 DB·Redis 접속 정보를 넣습니다 (git에 포함되지 않는 개인 설정 파일).
3. 실행합니다.

```bash
./gradlew bootRun
```

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- 헬스체크: `http://localhost:8080/actuator/health`

```bash
./gradlew test
```
