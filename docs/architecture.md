# 아키텍처 (초안)

PRD v2 10장 기준. 1차 설계(ERD, API 목록)가 정리되면 갱신한다.

## 형태

모듈러 모놀리스. 하나의 Spring Boot 앱 안에서 모듈을 나누고, 모듈 간에는 ID로 참조하며 내부 이벤트로 연결한다.

| 모듈 | 책임 |
| --- | --- |
| user | 소셜 로그인, JWT, 온보딩 프로필, 설정 |
| workout | 세션·종목·세트 기록, 오프라인 묶음 동기화, PR·주간 볼륨 |
| routine | 오늘의 운동 생성 (서버 규칙 무게 계산 + LLM 구성), 기본 루틴 대체 |
| notification | 알림 시각 예약, FCM 푸시 |
| challenge | 챌린지 선착순 신청 (Redis 원자 연산 + DB 유니크), 대기열, 인증·배지 |
| llm | LLM 호출 클라이언트, 프롬프트 버전, 호출 로그, 재시도·대체, 평가 세트 |

## 스택

- 서버: Kotlin, Spring Boot 3, Spring MVC, JPA, MySQL 8, Redis, Flyway(도입 예정)
- 앱: Flutter, Riverpod, Drift, FCM
- 운영: Docker, GitHub Actions, Prometheus·Grafana, k6
- 카프카, MSA, 쿠버네티스는 1차 비목표. 측정으로 필요가 확인될 때만 도입한다.

## 핵심 흐름

- **오늘의 운동:** 알림 30분 전 생성 → 후보 종목 SQL 필터 → LLM 구성 → 스키마·규칙 검증 → (실패 시 기본 루틴) → 캐시 → 알림 시각 FCM.
- **기록 동기화:** 앱 로컬 저장 → 최대 100건 묶음 전송 → `(user_id, client_id)` upsert, last-write-wins, tombstone 삭제 → 세션 종료 이벤트로 PR·볼륨 비동기 갱신.
- **챌린지 신청:** Redis Lua로 정원·중복 확인 → DB 유니크 제약 최후 안전망.

설계 판단은 `docs/decisions/`에 ADR로 남긴다.
