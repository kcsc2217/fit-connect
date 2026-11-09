# 008 로그인 확인용 Flutter 앱 구현 계획

- 근거: PRD 착수 계획 2단계(서버 API 하나 ↔ Flutter 화면 연결)·5단계(로그인 E2E), 명세 docs/specs/auth-jwt.md, docs/specs/auth-social-oidc.md
- 크기: M (외부 연동이 걸리지만 서버 코드는 바꾸지 않는다)
- 상태: 승인됨 (2026-11-09)

> 아래 코드는 계획 시점 초안이다. 테스트·구현 중 세부가 바뀔 수 있고, 크게 바뀌면 "변경 이력"에 남긴다.

## Context

1-B(007)는 가짜 공개키 서버로만 테스트됐다. 실제 카카오·구글 SDK가 준 id_token을 서버가 받는지 확인하려면 앱이 필요하다. 이 앱은 로그인 확인용 한 화면만 가진다. 온보딩 같은 다른 화면은 만들지 않는다.

## 범위

- `app/`에 Flutter 앱 하나를 둔다. 화면은 하나다.
  - DEV·카카오·구글 로그인 버튼
  - `POST /api/v1/auth/login` → `GET /api/v1/users/me` 결과 표시 (id, 닉네임, status, providers)
  - 방금 받은 id_token의 `iss`·`aud` 표시 (서버 허용 목록에 넣을 값 확인용)
  - 내 정보 다시 조회, 로그아웃
- 플랫폼은 **안드로이드만** 설정한다. 이 맥은 macOS 15.2라 최신 Xcode를 설치할 수 없다. Dart 코드는 공용이라 iOS는 빌드 환경이 생기면 Info.plist URL 스킴과 구글 iOS 클라이언트 ID만 붙이면 된다.
- 범위 밖: 다른 화면, iOS 설정, Flutter용 CI, refresh 동시 요청 묶기(single-flight).

## 접근

- **패키지:** `http`, `flutter_secure_storage`(안드로이드 Keystore 기반 암호화), `kakao_flutter_sdk_user`, `google_sign_in`(7.x).
- **키:** 카카오 네이티브 앱 키와 구글 클라이언트 ID는 앱 바이너리에 원래 들어가는 공개 식별자다. 그래도 개인 콘솔 앱에 저장소가 묶이지 않게 커밋하지 않는다.
  - Dart 쪽: `app/keys.json`(gitignore)을 `--dart-define-from-file`로 넣는다.
  - 안드로이드 매니페스트 쪽: `app/android/app/build.gradle.kts`가 같은 `keys.json`을 읽어 `manifestPlaceholders`로 넣는다 (키를 한 파일에만 둔다).
  - 예시 파일 `keys.example.json`만 커밋한다.
- **구글 aud 고정:** `serverClientId`에 웹 클라이언트 ID를 준다. 그러면 id_token의 `aud`가 플랫폼과 관계없이 웹 클라이언트 ID 하나가 된다. 서버 `GOOGLE_CLIENT_IDS`에는 그 값 하나만 넣으면 된다.
- **갱신:** 401 `code`가 `TOKEN_EXPIRED`일 때만 refresh를 한 번 시도한다. 교체된 refresh는 즉시 저장하고, refresh가 실패하면 저장소를 비운다.
- **서버 주소:** 기본값은 에뮬레이터에서 맥의 localhost를 가리키는 `http://10.0.2.2:8080`이다. 로컬 http만 허용하는 network security config를 둔다.

## 변경 파일

| 파일 | 변경 | 내용 |
| --- | --- | --- |
| app/ (flutter create) | 신규 | 안드로이드 플랫폼만 |
| app/pubspec.yaml | 수정 | 의존성 4개 |
| app/lib/config.dart | 신규 | `String.fromEnvironment`로 서버 주소·키 |
| app/lib/api/models.dart | 신규 | `LoginResult`, `TokenPair`, `Me` |
| app/lib/api/api_exception.dart | 신규 | ProblemDetail에서 `code`·`detail` 추출 |
| app/lib/api/auth_api.dart | 신규 | login, me, refresh, logout |
| app/lib/auth/token_store.dart | 신규 | `TokenStore` 인터페이스 + 보안 저장소 구현 |
| app/lib/auth/auth_repository.dart | 신규 | 로그인→저장→내 정보, TOKEN_EXPIRED 갱신, 로그아웃 |
| app/lib/auth/social_login.dart | 신규 | 카카오·구글 id_token 받기 |
| app/lib/util/jwt_payload.dart | 신규 | id_token payload 디코드 (표시용, 서명 검증 없음) |
| app/lib/main.dart | 신규 | 화면 하나 |
| app/android/... | 수정 | 카카오 리다이렉트 Activity, 인터넷 권한, 로컬 http 허용, 키 placeholder |
| app/test/*.dart | 신규 | 아래 테스트 목록 |
| app/README.md | 신규 | 실행법, 콘솔 등록 절차 |
| docs/auth-e2e-check.md | 신규 | 실제 토큰 확인 결과 (aud 값, 성공·실패, 서버에 넣을 값) |

## 테스트 목록

- `auth_api_test.dart`
  - 로그인 200 응답을 `LoginResult`로 읽는다
  - 401 ProblemDetail에서 `INVALID_SOCIAL_TOKEN` 코드를 읽는다
  - 본문이 JSON이 아니면 코드는 null이고 상태 코드만 남는다
  - 내 정보 요청에 Bearer 헤더를 붙인다
- `auth_repository_test.dart`
  - 로그인하면 토큰을 저장하고 내 정보를 돌려준다
  - TOKEN_EXPIRED면 갱신 후 새 토큰을 저장하고 한 번 재시도한다
  - UNAUTHORIZED면 갱신하지 않고 에러를 그대로 던진다
  - 갱신이 실패하면 저장소를 비운다
  - 로그아웃하면 서버 호출이 실패해도 저장소를 비운다
- `jwt_payload_test.dart`
  - aud가 문자열이든 배열이든 목록으로 읽는다
  - 형식이 잘못된 토큰은 null을 돌려준다

## 판단이 들어간 코드

### 갱신은 TOKEN_EXPIRED일 때만, 한 번만

```dart
Future<Me> fetchMe() async {
  final access = await _store.readAccess();
  if (access == null) throw const NotLoggedInException();
  try {
    return await _api.me(access);
  } on ApiException catch (e) {
    if (e.code != 'TOKEN_EXPIRED') rethrow;
    final refresh = await _store.readRefresh();
    if (refresh == null) rethrow;
    final TokenPair renewed;
    try {
      renewed = await _api.refresh(refresh);
    } on ApiException {
      await _store.clear();
      rethrow;
    }
    await _store.save(renewed.accessToken, renewed.refreshToken);
    return _api.me(renewed.accessToken);
  }
}
```

- 서명이 틀린 토큰(UNAUTHORIZED)은 갱신해도 소용없다 (ADR 0004).
- refresh는 쓸 때마다 교체되고 같은 값을 두 번 쓰면 401이라, 받은 즉시 저장한다.
- 화면 하나에서 요청이 동시에 나가지 않으므로 갱신 single-flight는 범위 밖이다. 본 앱에서는 필요하다.

### 구글 aud를 웹 클라이언트 ID로

```dart
GoogleSignIn.instance.initialize(serverClientId: AppConfig.googleServerClientId);
final account = await GoogleSignIn.instance.authenticate();
final idToken = account.authentication.idToken;
```

### 카카오 id_token이 없으면 앱에서 먼저 막는다

콘솔에서 OpenID Connect를 켜지 않으면 로그인은 성공하고 id_token만 비어 온다. 서버 401과 헷갈리지 않게 앱에서 원인을 알려 준다.

### ProblemDetail에서 code 읽기

화면 분기는 `detail` 문구가 아니라 `code`로 한다 (ADR 0003). 본문이 JSON이 아니면(프록시 오류 등) 상태 코드만 남긴다. 한글이 깨지지 않게 `utf8.decode(bodyBytes)`로 읽는다.

## 변경 이력

- 2026-11-09: 계획 초안은 iOS 시뮬레이터 기준이었으나, macOS 15.2에 최신 Xcode가 설치되지 않아 안드로이드 에뮬레이터로 바꿨다. Dart 코드와 테스트는 그대로다.
- 2026-11-09: 카카오 리다이렉트 스킴의 키를 `android/local.properties` 대신 `keys.json`에서 읽게 했다. 같은 키를 두 파일에 적다가 어긋나는 일을 막는다. 구글 iOS 클라이언트 ID 설정(`GOOGLE_IOS_CLIENT_ID`)은 iOS를 범위에서 빼면서 없앴다.
