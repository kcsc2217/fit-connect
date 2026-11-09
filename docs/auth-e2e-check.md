# 소셜 로그인 실제 토큰 확인 결과

명세 `docs/specs/auth-social-oidc.md`(계획 007)의 서버 검증을 **실제 카카오·구글 SDK가 준 id_token**으로 확인한 기록이다. 앱은 `app/`(계획 008)을 썼다.

- 환경: 안드로이드 에뮬레이터(API 37, Google Play 이미지) → 맥의 로컬 서버(`./gradlew bootRun`, local 프로파일)
- 앱은 `/api/v1/auth/login`에 id_token을 보내고, 받은 access로 `/api/v1/users/me`를 부른다. id_token의 `iss`·`aud`는 앱 화면에 디코드해 보여 준다(서명 검증은 서버만 한다).

## 카카오 (2026-11-09)

| 단계 | 서버 설정 | 결과 |
| --- | --- | --- |
| 1 | `fitness.auth.kakao.audiences` 없음 | 400 `UNSUPPORTED_PROVIDER` (명세대로 확인기를 만들지 않음) |
| 2 | `audiences` = 네이티브 앱 키 | **200**, `/users/me`의 `providers: [KAKAO]`, 새 사용자 `status: ONBOARDING` |

실제 id_token 값:

- `iss`: `https://kauth.kakao.com` (서버 `OidcProviderSettings` 기본값과 같다)
- `aud`: **네이티브 앱 키와 같은 값** (카카오 콘솔 > 앱 > 플랫폼 키 > 네이티브 앱 키)

콘솔에서 필요했던 것: 네이티브 앱 키에 Android 패키지명·키 해시 등록, 카카오 로그인 사용 설정 ON, **OpenID Connect ON**(끄면 id_token이 오지 않는다). 동의항목은 건드리지 않았다(서버는 `sub`만 쓰고 이메일을 받지 않는다).

## 구글

(확인 예정)

## 백엔드 확인 사항

- **서버에 넣을 값:** 카카오 `KAKAO_APP_KEYS` = 앱의 **네이티브 앱 키**. REST API 키가 아니다. 앱(SDK)을 초기화한 키가 aud로 온다.
- 로컬은 미커밋 `application-local.yml`에 넣는다.

  ```yaml
  fitness:
    auth:
      kakao:
        audiences: <네이티브 앱 키>
  ```

- 운영은 환경변수 `KAKAO_APP_KEYS`. iOS도 같은 카카오 앱의 같은 네이티브 앱 키를 쓰므로 값이 하나면 된다(iOS는 아직 미확인).
- 서명 키 지문(키 해시)은 빌드한 컴퓨터의 디버그 키스토어마다 다르다. 다른 컴퓨터에서 빌드하면 그 키 해시도 콘솔에 추가하거나 `~/.android/debug.keystore`를 복사한다. 서버 설정과는 무관하다.
- 확인 못 한 것: 다른 앱이 받은 카카오 토큰(aud 불일치)의 401은 실제 토큰으로 만들 수 없어 가짜 공개키 서버 테스트(007)에만 의존한다.
