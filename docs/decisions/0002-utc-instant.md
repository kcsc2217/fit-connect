# 0002 시각은 Instant + UTC로 저장, 벽시계 값은 변환 없이 저장

- 상태: 채택
- 날짜: 2026-11-01
- 관련: docs/db-schema.md 1장(시각, 사용자 기준 날짜), docs/plans/001-entity-rebuild-user.md

## 배경
스키마 원칙은 "시각은 `DATETIME(6)`에 UTC로 저장, 사용자 기준 날짜는 `DATE` 컬럼으로 따로"다.
이 서비스는 시간이 기능의 핵심이다. 알림 시각 30분 전 루틴 생성, "오늘"의 루틴, 자정 기준 챌린지 인증, 오프라인 기록의 수정 시각 비교(last-write-wins)가 모두 시각에 의존한다. 개발 PC는 Asia/Seoul이고, 운영 서버(VM·컨테이너)는 UTC일 가능성이 높다.

시간 값은 두 종류다.
- **순간**: 가입 시각, 세트 수행 시각. 어디서 보든 같은 한 순간이라 UTC로 통일해야 한다.
- **벽시계 값**: 알림 시각(18:30), 루틴 날짜(11월 1일). 사용자가 사는 곳 기준의 값 그 자체라 **변환하면 안 된다.**

## 선택지
### A. LocalDateTime + JVM 기본 시간대를 UTC로 고정
- 장점: Spring 예제에서 가장 흔하다.
- 단점: `LocalDateTime`에는 시간대 정보가 없어서, 값이 UTC인지 KST인지 코드만 보고 알 수 없다. JVM 옵션을 bootRun·테스트·운영마다 빠짐없이 넣어야 한다.

### B. Instant + Hibernate `jdbc.time_zone=UTC`
- 장점: 설정 한 줄로 Instant가 UTC로 저장된다.
- 단점: **벽시계 값까지 변환한다.** 처음에 이 방식을 썼다가 리뷰에서 발견했다. 서버가 KST일 때 알림 시각 18:30이 DB에 `09:30`으로 저장되고, 같은 서버에서 읽으면 18:30으로 되돌아와 앱 안에서는 드러나지 않는다. UTC 서버가 읽으면 09:30이 되고, `alarm_time`으로 대상자를 찾는 스케줄러 쿼리도 틀어진다. Hibernate 6.5가 `LocalTime`을 JVM 시간대 기준 `java.sql.Time`으로 만든 뒤 UTC Calendar로 넘기기 때문이다.

### C. Instant + DB 연결 시간대를 UTC로 고정
- 장점: 순간은 UTC로, 벽시계 값은 그대로 저장된다. DB 세션 시간대도 UTC라 SQL의 `NOW()`가 저장값과 같은 기준이 된다.
- 단점: 드라이버(Connector/J) 설정에 의존한다. 운영 JDBC URL에 `serverTimezone`·`connectionTimeZone`을 따로 넣으면 이 설정과 충돌할 수 있다.

## 결정
C. 시각 컬럼은 `Instant`, 사용자 기준 날짜는 `LocalDate`, 사용자 기준 시각(알림 시각)은 `LocalTime`.

```yaml
spring.datasource.hikari.data-source-properties:
  connectionTimeZone: UTC
  forceConnectionTimeZoneToSession: true
spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type: TIMESTAMP  # Instant → DATETIME(6)
```

`hibernate.jdbc.time_zone`은 쓰지 않는다. 감사 시각은 `Instant.now()`를 주는 `DateTimeProvider`로 채운다.

## 이유
테스트를 Asia/Seoul JVM에서 돌려 DB 원본 값을 직접 읽어 확인했다.

| 설정 | Instant `2026-11-01T00:00:00Z` | 알림 시각 18:30 |
| --- | --- | --- |
| B. `jdbc.time_zone=UTC` | `2026-11-01 00:00:00` (정상) | **`09:30:00`** (밀림) |
| C. 연결 시간대 UTC | `2026-11-01 00:00:00` (정상) | `18:30:00` (정상) |
| C에서 연결 시간대 설정만 뺌 | **실패** (KST로 저장) | - |

Hibernate의 `java_time_use_direct_jdbc`(실험 단계 설정)도 검토했지만, C만으로 두 테스트가 통과해서 넣지 않았다.

## 결과
- 테스트 JVM은 항상 `Asia/Seoul`로 돈다(`build.gradle.kts`). UTC 장비(CI)에서 시간대 버그가 숨지 않게 하기 위해서다.
- 시간 값을 다루는 컬럼을 추가하면 DB 원본 값을 읽는 테스트를 함께 쓴다 (`BaseTimeEntityTest`, `UserProfileStorageTest`). 002의 `LocalDate`(week_start, routine_date)도 같은 방식으로 확인한다.
- JDBC URL에 `serverTimezone`·`connectionTimeZone`을 넣지 않는다.
- "오늘"·"자정" 판단은 반드시 `user_profile.timezone`으로 변환해서 한다. 서버 시간대로 `LocalDate.now()`를 쓰지 않는다.
- MySQL에서 직접 조회한 시각은 UTC다. 운영 확인 시 `CONVERT_TZ`로 변환해서 본다.
