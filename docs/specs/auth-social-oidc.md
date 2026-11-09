# 인증 1-B: 카카오·구글 소셜 로그인 (OIDC id_token 검증)

- 요구사항: FR-01 (P0, 카카오·구글. 애플은 iOS 출시 시), FR-02 (P0, 소셜 토큰 서버 검증), 비기능 "보안", "성능: 일반 API p95 300ms 이하"
- 상태: 확정 (2026-11-06)
- 선행: docs/specs/auth-jwt.md (1-A), docs/decisions/0004-auth-token.md

## 목적
사용자가 카카오나 구글 계정으로 로그인한다. 서버는 앱이 보낸 소셜 토큰이 정말 그 회사가 우리 앱에 발급한 것인지 확인한 뒤에만 우리 토큰(1-A)을 발급한다.

## 범위
- 포함:
  - 카카오 id_token 검증 (`SocialTokenVerifier` 구현 추가)
  - 구글 id_token 검증 (`SocialTokenVerifier` 구현 추가)
  - 공개키(JWKS) 조회와 캐싱, 키 교체(kid가 바뀜) 대응, 외부 호출 타임아웃
  - 허용할 앱 식별자(aud) 목록을 설정으로 받기 (비밀값은 아니지만 환경마다 다름)
  - `social_account.provider_user_id`를 대소문자·끝 공백을 구분하도록 변경 (새 마이그레이션, 006 리뷰 지적 5)
  - id_token을 쓰기로 한 결정을 ADR로 남긴다 (access_token으로 사용자 정보 API를 부르는 방식과 비교)
- 제외 (다른 단계·기능):
  - 애플 로그인 → iOS 출시 시 (FR-01)
  - 한 사람의 카카오·구글 계정 연결(같은 회원으로 묶기) → db-schema 열린 질문. 1-B에서는 서로 다른 회원이 된다
  - 로그인 API 형태 변경 → 없음. 1-A의 `POST /api/v1/auth/login`을 그대로 쓰고 `provider`만 `KAKAO`·`GOOGLE`
  - 앱(Flutter)의 소셜 SDK 연동 → 앱 작업. 서버는 앱이 받은 id_token만 받는다
  - 실제 카카오·구글 토큰으로 배포 서버에서 끝까지 확인 → 착수 계획 5단계 "로그인 E2E"
  - 회원 탈퇴 시 소셜 연결 끊기(카카오 연결 끊기 API 등) → FR-04 탈퇴 작업

## 동작

### 로그인 (카카오·구글 공통)
1. 앱이 소셜 SDK로 로그인해 id_token을 받는다.
2. 앱이 `POST /api/v1/auth/login {"provider": "KAKAO" | "GOOGLE", "token": "<id_token>"}`을 보낸다.
3. 서버가 provider에 맞는 확인기로 id_token을 검증한다.
   - 서명: 토큰 헤더의 `kid`로 그 회사의 공개키를 골라 RS256 서명을 확인한다. 알고리즘은 서버가 RS256으로 고정한다.
   - `iss`: 카카오는 `https://kauth.kakao.com`. 구글은 `https://accounts.google.com` 또는 `accounts.google.com`.
   - `aud`: 설정에 등록한 우리 앱 식별자 중 하나 (카카오는 앱 키, 구글은 OAuth 클라이언트 ID).
   - `exp`: 현재 시각보다 뒤 (시계 오차는 짧게 허용).
   - `sub`: 비어 있지 않고 100자 이하 (`provider_user_id` 컬럼 길이).
4. 통과하면 `(provider, sub)`로 1-A의 로그인 흐름(회원 찾기 또는 가입 → 토큰 발급)을 그대로 탄다.

### 공개키 관리
- 공개키 목록은 회사별 주소에서 받아 캐시한다 (카카오 `https://kauth.kakao.com/.well-known/jwks.json`, 구글 `https://www.googleapis.com/oauth2/v3/certs`).
- 캐시에 없는 `kid`가 오면 (회사가 키를 교체한 경우) 다시 받아 본다. 다만 연속으로 계속 다시 받지는 않는다 (카카오는 지나치게 잦은 요청을 차단할 수 있다고 안내한다).
- 캐시는 서버마다 메모리에 둔다. 로그인 상태가 아니라 회사가 공개한 키의 사본이라 "서버 메모리 상태 금지" 원칙의 대상이 아니다 (서버마다 따로 받아도 결과가 같다). (열린 질문 아님, 계획에서 확인)

## 규칙과 제약
- 사용자 식별자는 `sub`만 쓴다. 이메일은 바뀔 수 있어 식별자로 쓰지 않는다 (구글 문서 권고).
- 서명 알고리즘은 RS256만 받는다. `alg: none`, HS256(공개키를 비밀키로 쓰는 공격) 등은 거절한다.
- 허용 aud가 하나도 설정되지 않은 provider는 확인기를 만들지 않는다 (그 provider 로그인은 400 `UNSUPPORTED_PROVIDER`). 설정을 빠뜨린 채 아무 앱의 토큰이나 받아 주는 일이 없게 한다.
- 외부 호출(공개키 조회)은 짧은 타임아웃을 둔다. 로그인 요청마다 외부를 부르지 않도록 캐시가 대부분의 요청을 처리해야 한다 (p95 300ms).
- id_token 원문과 이메일은 로그에 남기지 않는다. 이메일은 저장하지도 않는다.
- 공개키 검증 라이브러리는 Nimbus(`spring-security-oauth2-jose`)를 쓴다. 우리 토큰(1-A)은 jjwt를 그대로 쓴다.
- `provider_user_id` 비교는 대소문자와 끝 공백을 구분한다 (`Abc`와 `abc`는 다른 사용자).
- 1-A의 규칙은 그대로다: 같은 소셜 계정의 동시 첫 로그인은 회원 1명, 기본 닉네임은 서버가 채움, access·refresh 발급 방식 동일.

## 예외 상황
| 상황 | 기대 동작 (응답 코드·에러 코드) |
| --- | --- |
| 서명 불일치, 형식 오류, RS256이 아닌 알고리즘 | 401 `INVALID_SOCIAL_TOKEN` |
| `iss` 불일치 (다른 회사 토큰을 다른 provider로 보냄 포함) | 401 `INVALID_SOCIAL_TOKEN` |
| `aud` 불일치 (다른 앱이 받은 토큰) | 401 `INVALID_SOCIAL_TOKEN` |
| `exp` 지남 | 401 `INVALID_SOCIAL_TOKEN` (앱이 소셜 SDK로 다시 로그인) |
| 공개키 목록에 없는 `kid` (다시 받아도 없음) | 401 `INVALID_SOCIAL_TOKEN` |
| `sub` 없음 또는 100자 초과 | 401 `INVALID_SOCIAL_TOKEN` |
| 공개키 조회 실패·타임아웃 (회사 쪽 장애) | 503 `SOCIAL_LOGIN_UNAVAILABLE` (앱은 "잠시 후 다시 시도" 안내, 소셜 로그인을 다시 시키지 않음) |
| 허용 aud가 설정되지 않은 provider | 400 `UNSUPPORTED_PROVIDER` |
| 같은 id_token을 다시 보냄 (재전송·탈취) | 수명 안이면 통과 (추가 검사 없음, 열린 질문 1. 위험은 ADR에 기록) |
| 같은 사람이 카카오와 구글로 각각 로그인 | 서로 다른 회원 2명 (계정 연결은 범위 밖) |

## 데이터
- `social_account`:
  - `provider`: `KAKAO`, `GOOGLE` 사용 (enum에 이미 있음)
  - `provider_user_id`: id_token의 `sub`. **콜레이션을 `utf8mb4_0900_bin`(NO PAD, 끝 공백도 구분)으로 바꾸는 새 마이그레이션(V6).** `utf8mb4_bin`은 MySQL 8에서 PAD SPACE라 끝 공백을 무시해 쓰지 않는다 (계획 007). 엔티티 매핑과 유니크 제약은 그대로
  - `email`: 저장하지 않는다 (null). 지금 이메일을 쓰는 기능이 없어 개인정보를 덜 모은다 (열린 질문 2)
- 새 테이블·Redis 키 없음.
- 설정 (비밀값 아님, 환경마다 다름): 카카오 허용 앱 키 목록, 구글 허용 클라이언트 ID 목록. 운영은 환경변수로 받는다.

## API 초안
| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| POST | /api/v1/auth/login | 변경 없음. `provider`에 `KAKAO`·`GOOGLE`, `token`에 id_token |


## 경계
- 1-A (`user` 모듈): `SocialTokenVerifier` 구현 두 개를 추가할 뿐 로그인·토큰 흐름은 바꾸지 않는다.
- 앱: 카카오는 개발자 콘솔에서 OpenID Connect를 켜야 id_token이 나온다. 구글은 앱이 id_token을 받을 때 쓰는 클라이언트 ID가 서버 허용 목록에 있어야 한다. 앱 설정 값은 서버 설정과 짝을 맞춘다.
- 탈퇴(FR-04): 카카오 연결 끊기 같은 소셜 쪽 처리는 탈퇴 작업에서 다룬다.
- 운영 배포: 허용 aud 목록과 외부 호출(카카오·구글 공개키 주소로 나가는 HTTPS)이 열려 있어야 한다.

## 열린 질문
- [x] 1. id_token 재사용(replay) 방지 → 추가 검사 없음. aud·exp 검증만 한다. 토큰은 앱↔회사↔서버 사이 HTTPS로만 오가고 탈취돼도 수명(구글 약 1시간, 카카오 약 2시간) 안에서만 쓸 수 있다. 위험은 ADR에 남긴다
- [x] 2. 소셜 이메일 저장 → 저장하지 않는다. 필요한 기능이 생기면 그때 추가한다
- [x] 3. 공개키 조회 실패(회사 쪽 장애) → 503 `SOCIAL_LOGIN_UNAVAILABLE`. 토큰 문제(401)와 구분해 앱이 재시도를 안내한다
- [x] 4. 검증 라이브러리 → Nimbus(`spring-security-oauth2-jose`). 공개키 조회·캐시·모르는 kid 재조회·타임아웃이 내장돼 있다. 1-A의 jjwt는 우리 토큰에 계속 쓴다
