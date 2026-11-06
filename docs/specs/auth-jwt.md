# 인증 1-A: JWT 발급·갱신·로그아웃과 내 정보 조회

- 요구사항: FR-02 (P0), FR-01의 서버 쪽 토대 (P0), 비기능 "보안: 리프레시 토큰 교체", "확장성: 서버 메모리 상태 금지"
- 상태: 확정 (2026-11-04)

## 목적
사용자가 한 번 로그인하면 이후 요청마다 다시 로그인하지 않고 앱을 쓸 수 있다. 서버는 모든 API에서 "누가 요청했는지"(userId)를 안전하게 안다.

## 범위
- 포함:
  - 로그인: 소셜 신원 확인(인터페이스) → 회원 찾기 또는 가입 → access·refresh 발급
  - 개발용 로그인: 로컬·테스트 설정에서만 켜지는 신원 확인 구현 (소셜 검증 없이 통과)
  - 갱신: refresh로 새 access·refresh 발급, 옛 refresh 폐기 (교체)
  - 로그아웃: refresh 폐기
  - 요청마다 access 검증 (Spring Security 필터), 인증이 필요한 경로와 아닌 경로 구분
  - 401·403을 ProblemDetail + code로 응답 (ADR 0003 형식)
  - 내 정보 조회
- 제외 (다른 단계·기능):
  - 카카오·구글 id_token 검증 (OIDC) → 1-B
  - 애플 로그인 → iOS 출시 시 (FR-01)
  - 회원 탈퇴 API와 데이터 삭제 정책 → FR-04 (PRD 열린 질문 "탈퇴 시 데이터 보관·삭제 정책"이 정해진 뒤)
  - 여러 소셜 계정 연결 → db-schema 열린 질문
  - 온보딩 프로필 저장 → 2번 온보딩
  - 관리자 권한(역할) → 필요해질 때. 1-A는 권한 구분 없이 "인증됨/안 됨"만 본다

## 동작

### 로그인
1. 앱이 `provider`와 소셜 토큰을 보낸다.
2. 서버가 provider에 맞는 신원 확인기로 토큰을 검증해 `(provider, providerUserId)`를 얻는다. 1-A에서는 개발용 확인기만 있다.
3. `social_account`에서 `(provider, providerUserId)`로 회원을 찾는다. 없으면 `users`(status = ONBOARDING)와 `social_account`를 만든다.
4. access(30분)와 refresh(14일)를 발급하고 refresh를 Redis에 저장한다.
5. 응답: access, refresh, 각 만료 시각, 회원 상태(앱이 온보딩 화면으로 갈지 정하는 데 씀).

### 인증이 필요한 API 요청
1. 앱이 `Authorization: Bearer <access>`를 보낸다.
2. 필터가 서명·알고리즘·만료를 확인하고 userId를 꺼낸다. DB·Redis는 조회하지 않는다.
3. 컨트롤러는 userId를 파라미터로 받는다.

### 갱신
1. 앱이 refresh를 보낸다.
2. 서버가 Redis에서 refresh를 **꺼내면서 지운다** (한 번만 쓸 수 있음).
3. 있으면 새 access·refresh를 발급하고 새 refresh를 저장한다. 없으면 401.

### 로그아웃
1. 앱이 refresh를 보낸다.
2. 서버가 Redis에서 지운다. 이미 없어도 성공으로 응답한다 (같은 요청을 다시 보내도 결과가 같게).
3. access는 만료될 때까지 유효하다 (앱이 기기에서 지운다).

### 내 정보 조회
1. access의 userId로 회원을 조회해 id, 닉네임, 상태, 가입 시각, 연결된 소셜 종류를 준다.

## 규칙과 제약
- access: JWT, HS256, 수명 30분, claim은 `sub`(userId), `iat`, `exp`만. 이메일 등 개인정보를 넣지 않는다.
- refresh: JWT가 아닌 추측 불가능한 랜덤 값. Redis 키 `refresh:{userId}:{tokenId}`, TTL 14일 (db-schema 4장).
- 한 사용자가 여러 기기에서 동시에 로그인할 수 있다 (기기마다 refresh가 따로 있음, 키 구조가 이를 전제함).
- 갱신마다 refresh를 교체한다. 같은 refresh로 두 번 갱신하면 두 번째는 실패한다 (Redis 원자 연산으로 꺼내며 삭제).
- 이미 교체된 refresh가 다시 들어오면 그 요청만 401로 끝낸다. 그 사용자의 다른 refresh는 건드리지 않는다 (피해 범위는 access 수명 30분으로 제한).
- 가입 시 닉네임은 서버가 기본값으로 채운다 (예: `회원` + 랜덤 숫자). 바꾸는 기능은 설정·온보딩에서 다룬다.
- 서명 키는 코드·커밋 파일에 두지 않는다. 32바이트 이상. 운영은 환경변수, 로컬은 미커밋 `application-local.yml`.
- 검증할 때 알고리즘은 서버가 고정한다 (토큰 헤더의 `alg`를 믿지 않는다). 시계 오차 허용은 짧게 둔다.
- 개발용 로그인은 설정 `fitness.auth.dev-login.enabled=true`일 때만 빈이 생긴다. 이 설정은 로컬(`application-local.yml`, jar에서 제외)과 테스트 설정에만 둔다. 운영에서 호출하면 지원하지 않는 provider로 400이다. (프로파일 조건은 기본 활성 프로파일이 local이라 운영에서 프로파일 지정을 빠뜨리면 열리므로 쓰지 않는다. 계획 006 1-6)
- 서버 메모리에 로그인 상태를 두지 않는다. refresh만 Redis에 둔다.
- 같은 소셜 계정으로 동시에 처음 로그인해도 회원은 하나만 생긴다 (`social_account (provider, provider_user_id)` 유니크 제약이 최후 안전망).
- 성능: access 검증은 저장소 조회 없이 끝난다. 로그인·갱신은 일반 API p95 300ms 이하 목표에 포함.

## 예외 상황
| 상황 | 기대 동작 (응답 코드·에러 코드) |
| --- | --- |
| 인증이 필요한 API에 토큰 없음 | 401 `UNAUTHORIZED` |
| access 서명 위조, 형식 오류, 다른 알고리즘 | 401 `UNAUTHORIZED` |
| access 만료 | 401 `TOKEN_EXPIRED` (앱은 이 코드일 때만 갱신을 시도) |
| 갱신 시 refresh가 Redis에 없음 (만료, 로그아웃, 이미 교체됨) | 401 `INVALID_REFRESH_TOKEN` |
| 로그아웃 시 refresh가 이미 없음 | 204 (성공) |
| 지원하지 않는 provider (운영에서 DEV 포함) | 400 `UNSUPPORTED_PROVIDER` (enum에 없는 문자열은 400 `INVALID_INPUT`) |
| 소셜 토큰 검증 실패 (1-B에서 실제로 발생) | 401 `INVALID_SOCIAL_TOKEN` |
| 같은 소셜 계정 첫 로그인이 동시에 두 번 | 회원 1명. 늦은 요청도 같은 회원으로 토큰 발급 |
| 탈퇴(WITHDRAWN) 회원의 로그인·갱신·조회 | 이번 범위 밖. 탈퇴 API를 만들 때 refresh 전부 폐기와 함께 정한다 (지금은 WITHDRAWN 회원을 만드는 경로가 없음) |
| 권한 부족 | 403 `FORBIDDEN` (1-A에는 발생 경로 없음, 형식만 맞춤) |

## 데이터
- 기존 테이블 사용, 스키마 변경 없음.
  - `users`: 가입 시 status = ONBOARDING, nickname은 서버 기본값
  - `social_account`: `(provider, provider_user_id)` 유니크, `(user_id)` 인덱스
- `AuthProvider` enum에 개발용 값 `DEV`를 추가한다 (열린 질문 3). 컬럼은 VARCHAR(20)이라 마이그레이션은 필요 없다.
- Redis: `refresh:{userId}:{tokenId}` → TTL 14일.

## API 초안
| 메서드 | 경로 | 인증 | 설명 |
| --- | --- | --- | --- |
| POST | /api/v1/auth/login | 없음 | 소셜 토큰으로 로그인·가입, access·refresh 발급 |
| POST | /api/v1/auth/refresh | 없음 | refresh로 재발급 (교체) |
| POST | /api/v1/auth/logout | 없음 (refresh로 식별) | refresh 폐기 |
| GET | /api/v1/users/me | 필요 | 내 정보 조회 |

- 인증 없이 허용: `/api/v1/auth/**`, `/actuator/health`, Swagger, 테스트 전용 `/test-support/**` (test 프로파일).

## 경계
- 다른 모든 모듈: 컨트롤러에서 userId(Long)만 받는다. Spring Security 타입을 서비스에 넘기지 않는다.
- common: 401·403 응답은 `common.exception`의 ProblemDetail 형식과 `ErrorCode`를 쓴다. 새 코드(UNAUTHORIZED 등)를 추가한다.
- 2번 온보딩: 내 정보 조회의 status(ONBOARDING/ACTIVE)로 앱이 화면을 정한다. ACTIVE 전환은 온보딩이 한다.
- 1-B: 신원 확인기 인터페이스에 카카오·구글 구현을 추가한다. 로그인 이후 흐름은 바꾸지 않는다.
- analytics: 로그인 이벤트 기록은 이번 범위가 아니다.

## 열린 질문
- [x] 1. 이미 교체된(재사용된) refresh가 들어오면 → 그 요청만 401. 사용자별 refresh 목록 관리가 필요 없고, 네트워크 재시도로 전 기기가 로그아웃되는 일을 피한다
- [x] 2. 가입 시 닉네임 → 서버가 자동 기본값으로 채운다 (1-A·1-B 같은 규칙)
- [x] 3. 개발용 로그인의 provider → `AuthProvider.DEV`를 추가한다. 가짜 KAKAO 계정이 진짜 카카오 회원과 섞이지 않게 한다. 운영 데이터에 DEV 행이 생기지 않는 것은 설정값(개발용 확인기 빈이 로컬·테스트 설정에서만 생김)과 테스트로 보장한다
- [x] 4. access 만료와 무효 구분 → 구분한다. 만료 `TOKEN_EXPIRED`, 그 밖의 무효·없음 `UNAUTHORIZED`
- [x] 5. 탈퇴(WITHDRAWN) 회원 → 탈퇴 API 작업에서 함께 처리한다. 1-A에는 넣지 않는다
