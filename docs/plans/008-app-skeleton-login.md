# 008 앱 뼈대: 로그인 → 내 정보 (Flutter, iOS) 구현 계획

- 명세: docs/specs/app-skeleton-login.md (확정 2026-11-12)
- 크기: M
- 상태: 승인됨 (2026-11-12)
- 브랜치: `feat/app-skeleton` (docs/workflow.md "브랜치와 머지")

> 아래 코드는 계획 시점 초안이다. 테스트·구현 중 세부가 바뀔 수 있고, 크게 바뀌면 "변경 이력"에 남긴다.

## Context

서버에는 로그인·갱신·로그아웃·내 정보 API 4개가 있지만 이를 호출하는 앱이 없다. 이번 작업에서는 저장소 `app/` 폴더에 Flutter 앱을 만든다. 범위는 로그인 → 메인 탭 → "나" 탭 내 정보까지이며, 이후 모든 화면이 올라갈 바탕(폴더 구조, 테마, 서버 호출·인증 처리, 테스트, CI, 훅)을 함께 정한다. 서버 코드는 바꾸지 않는다.

## 접근

- **기능별 폴더, 기능 안은 층별:** 서버의 "모듈별 패키지, 모듈 안은 레이어별" 규칙을 앱에 맞게 옮긴다. `lib/core/`(설정, 네트워크, 저장소, 테마, 공통 위젯)와 `lib/features/<기능>/`(`data/` 서버·SDK 호출, `domain/` 모델·상태, `presentation/` 화면)으로 나눈다.
- **Riverpod는 코드 생성 없이 쓴다:** `build_runner`를 쓰지 않고 `Provider`, `Notifier`, `AsyncNotifier`를 손으로 정의한다. Dart가 처음이라 생성 코드가 없는 쪽이 읽기 쉽다. 필요해지면 나중에 바꾼다.
- **토큰 갱신은 한 곳에서 한 번만:** Dio 인터셉터가 401 `TOKEN_EXPIRED`를 받으면 `TokenRefresher.refresh()`를 부른다. 이 메서드는 진행 중인 갱신이 있으면 같은 `Future`를 돌려준다. 그래서 동시에 여러 요청이 만료돼도 `/auth/refresh`는 한 번만 나간다. 갱신 요청은 인터셉터가 없는 별도 Dio로 보내 재귀를 막는다. Dio의 `QueuedInterceptor`는 에러를 줄 세울 뿐 "이미 갱신됐는지"는 알려주지 않는다. 그래서 갱신 여부를 직접 판단하는 쪽이 테스트로 검증하기 쉽다.
- **인증 상태 하나로 화면을 정한다:** `AuthController`(`AsyncNotifier<AuthState>`)가 `signedOut / onboarding / active / startupError` 중 하나를 갖는다. `go_router`의 `redirect`가 이 상태만 보고 로그인·온보딩 자리·메인 탭으로 보낸다. 세션이 만료되면(갱신 실패) 인터셉터가 `AuthController.sessionExpired()`를 부르고, 상태가 바뀌면 화면은 자동으로 로그인으로 간다.
- **소셜 SDK는 인터페이스 뒤로 감춘다:** `SocialLoginClient { provider; Future<String?> signIn(); Future<void> signOut(); }`. `signIn()`의 반환값은 id_token이고, 사용자가 창을 닫으면 `null`이다. 카카오·구글·개발용 구현 세 개를 두고, 테스트에서는 가짜로 바꿔 끼운다.
- **설정 값은 두 군데, 둘 다 커밋하지 않는다:** Dart 쪽 값(서버 주소, 키, 개발용 로그인 여부)은 `--dart-define-from-file=config/local.json`으로 넘긴다. iOS 네이티브 쪽 값(카카오·구글 URL 스킴, 구글 클라이언트 ID)은 Info.plist가 읽어야 해서 `ios/Flutter/Secrets.xcconfig`에 둔다. 두 파일 모두 gitignore 대상이고, `*.example` 파일을 커밋한다. 같은 키를 두 곳에 적어야 하는 번거로움이 있지만, Flutter가 dart-define을 Info.plist로 넘겨주는 공식 방법이 없어 가장 단순한 방식을 택했다.

## 변경 파일

| 파일 | 변경 | 내용 |
| --- | --- | --- |
| app/ (`flutter create --platforms ios --org com.fitness`) | 신규 | 프로젝트 생성. 번들 ID는 생성 후 `com.fitness.trainerssaem`으로 고친다 (기본 생성값은 `trainerSsaem`처럼 대문자가 섞임) |
| app/pubspec.yaml | 신규 | flutter_riverpod, dio, go_router, flutter_secure_storage, kakao_flutter_sdk_user, google_sign_in, intl / 테스트: mocktail. Pretendard 글꼴 등록 |
| app/assets/fonts/Pretendard-*.otf | 신규 | Regular·Medium·SemiBold·Bold·ExtraBold (OFL 라이선스, 공식 GitHub 릴리스에서 받음. 받을 때 다시 확인받음) |
| app/config/example.json, app/config/local.json(미커밋) | 신규 | `API_BASE_URL`, `KAKAO_NATIVE_APP_KEY`, `GOOGLE_IOS_CLIENT_ID`, `DEV_LOGIN_ENABLED`, `DEV_LOGIN_TOKEN` |
| app/ios/Flutter/Secrets.example.xcconfig, Secrets.xcconfig(미커밋) | 신규 | `KAKAO_NATIVE_APP_KEY`, `GOOGLE_IOS_CLIENT_ID`, `GOOGLE_REVERSED_CLIENT_ID`. Debug/Release.xcconfig에서 `#include? "Secrets.xcconfig"`로 읽음 (파일이 없어도 빌드는 됨) |
| app/ios/Runner/Info.plist | 수정 | URL 스킴(`kakao$(KAKAO_NATIVE_APP_KEY)`, `$(GOOGLE_REVERSED_CLIENT_ID)`), `GIDClientID`, `LSApplicationQueriesSchemes`(카카오톡), `NSAllowsLocalNetworking` |
| app/lib/main.dart, app/lib/app.dart | 신규 | `ProviderScope`, 카카오 SDK 초기화, `MaterialApp.router`, 다크 테마 |
| app/lib/core/config/app_config.dart | 신규 | `String.fromEnvironment`로 설정 값 읽기 |
| app/lib/core/theme/ (app_colors, app_theme) | 신규 | 레퍼런스 색·글꼴·모서리 토큰, `ThemeData` |
| app/lib/core/widgets/ (primary_button, app_card, coming_soon_view) | 신규 | 라임 알약 버튼, 카드, "준비 중" 화면 |
| app/lib/core/network/api_exception.dart | 신규 | `ApiException(status, code, detail)`, `NetworkException`, `UnknownApiException`. ProblemDetail 해석 |
| app/lib/core/network/dio_provider.dart | 신규 | 인증 Dio(인터셉터 포함), 인증 없는 Dio(로그인·갱신·로그아웃용), 타임아웃 |
| app/lib/core/network/auth_interceptor.dart | 신규 | access 헤더 첨부, `TOKEN_EXPIRED` → 갱신 → 1회 재시도, 그 밖의 401 → 세션 만료 |
| app/lib/core/storage/token_storage.dart | 신규 | Keychain(flutter_secure_storage)에 토큰 4개 읽기·쓰기·삭제 |
| app/lib/features/auth/data/auth_api.dart | 신규 | login, refresh, logout 호출과 DTO |
| app/lib/features/auth/data/token_refresher.dart | 신규 | 갱신을 한 번만 보내기 (아래 스케치) |
| app/lib/features/auth/data/social/ (social_login_client, kakao_, google_, dev_) | 신규 | 소셜 SDK 감싸기 |
| app/lib/features/auth/domain/auth_controller.dart | 신규 | 앱 시작 판정, 로그인, 로그아웃, 세션 만료, 온보딩 건너뛰기 |
| app/lib/features/auth/presentation/ (login_screen, onboarding_placeholder_screen, startup_error_screen) | 신규 | 화면 3개 |
| app/lib/features/me/ (data/me_api, presentation/me_screen) | 신규 | 내 정보 조회와 "나" 탭 |
| app/lib/features/shell/main_shell.dart | 신규 | 하단 탭 5개 (`StatefulShellRoute.indexedStack`) |
| app/lib/router.dart | 신규 | 라우트 정의와 인증 상태 기반 redirect |
| app/test/... | 신규 | 아래 테스트 목록. `test/support/`에 가짜 HTTP 어댑터, 가짜 소셜 클라이언트, 메모리 토큰 저장소 |
| app/analysis_options.yaml | 신규 | flutter_lints + 몇 개 규칙 (상대 import 대신 package import 등) |
| .gitignore | 수정 | `app/config/local.json`, `app/ios/Flutter/Secrets.xcconfig` |
| .github/workflows/ci.yml | 수정 | `app` 작업 추가: 바뀐 파일 중 `app/`이 있을 때만 `flutter analyze` + `flutter test`. 서버 `test` 작업은 그대로 둔다 |
| .claude/hooks/format-and-compile.sh | 수정 | `*.dart` 수정 시 `dart format` + `flutter analyze` (실패하면 exit 2) |
| .claude/hooks/run-tests.sh | 수정 | `app/`에 변경이 있으면 `flutter test`도 돌림 (서버와 따로 지문 관리) |
| CLAUDE.md | 수정 | 앱 섹션: 폴더 구조, 실행 명령어, 설정 파일, 테스트 규칙(Dart판 Given-When-Then, mocktail) |
| docs/specs/app-skeleton-login.md | 수정 | 아래 "확인 필요" 결과 반영 (ATS 범위, CI 경로 분리 방식) |

## 데이터 변경

서버 없음. 기기 Keychain 키: `access_token`, `access_token_expires_at`, `refresh_token`, `refresh_token_expires_at`.

## API

서버 API는 기존 그대로다 (auth-jwt.md, auth-social-oidc.md).

| 메서드 | 경로 | 요청 | 응답 | 앱이 분기하는 에러 code |
| --- | --- | --- | --- | --- |
| POST | /api/v1/auth/login | `{provider, token}` | 토큰 4개 + `status` | `INVALID_SOCIAL_TOKEN`, `SOCIAL_LOGIN_UNAVAILABLE`, `UNSUPPORTED_PROVIDER` |
| POST | /api/v1/auth/refresh | `{refreshToken}` | 토큰 4개 | `INVALID_REFRESH_TOKEN` |
| POST | /api/v1/auth/logout | `{refreshToken}` | 204 | (무시) |
| GET | /api/v1/users/me | Bearer | `{id, nickname, status, createdAt, providers}` | `TOKEN_EXPIRED`, `UNAUTHORIZED` |

## 핵심 코드 스케치

### 갱신을 한 번만 보내기 (`token_refresher.dart`)

```dart
class TokenRefresher {
  TokenRefresher(this._authApi, this._storage);
  final AuthApi _authApi;          // 인터셉터 없는 Dio 사용
  final TokenStorage _storage;
  Future<String>? _inFlight;       // 진행 중인 갱신 (앱 프로세스 안에서만, 서버 상태 아님)

  /// 새 access를 돌려준다. 동시에 불려도 /auth/refresh는 한 번만 나간다.
  /// 실패하면 SessionExpiredException (refresh 없음, INVALID_REFRESH_TOKEN, 갱신 응답 401).
  /// 네트워크 실패는 NetworkException 그대로 (토큰을 지우지 않음).
  Future<String> refresh() {
    return _inFlight ??= _doRefresh().whenComplete(() => _inFlight = null);
  }

  Future<String> _doRefresh() async {
    final refreshToken = await _storage.readRefreshToken();
    if (refreshToken == null) throw SessionExpiredException();
    try {
      final tokens = await _authApi.refresh(refreshToken);
      await _storage.save(tokens);
      return tokens.accessToken;
    } on ApiException catch (e) {
      if (e.status == 401) throw SessionExpiredException();
      rethrow;
    }
  }
}
```

### 인터셉터 분기 (`auth_interceptor.dart`)

```dart
@override
Future<void> onError(DioException err, ErrorInterceptorHandler handler) async {
  final res = err.response;
  final alreadyRetried = err.requestOptions.extra['retried'] == true;
  if (res?.statusCode != 401 || alreadyRetried) return handler.next(err);

  final code = ApiException.codeOf(res!);            // ProblemDetail의 code
  if (code != 'TOKEN_EXPIRED') {                      // UNAUTHORIZED 등 → 갱신하지 않음
    await _onSessionExpired();                        // 토큰 삭제 + AuthController.sessionExpired()
    return handler.next(err);
  }
  try {
    final newAccess = await _refresher.refresh();     // 동시에 와도 한 번만
    final retry = err.requestOptions
      ..headers['Authorization'] = 'Bearer $newAccess'
      ..extra['retried'] = true;
    return handler.resolve(await _dio.fetch(retry));  // 재시도는 1회. 또 401이면 위 alreadyRetried에서 끝
  } on SessionExpiredException {
    await _onSessionExpired();
    return handler.next(err);
  } on NetworkException {
    return handler.next(err);                         // 토큰 유지
  }
}
```

### 앱 시작 판정 (`auth_controller.dart`)

```dart
@override
Future<AuthState> build() async {
  if (await _storage.readRefreshToken() == null) return const AuthState.signedOut();
  try {
    final me = await _meApi.fetch();                  // 만료면 인터셉터가 갱신
    return AuthState.fromStatus(me.status);           // ACTIVE → active, ONBOARDING → onboarding
  } on SessionExpiredException {
    return const AuthState.signedOut();               // 토큰은 인터셉터가 이미 지움
  } on NetworkException {
    return const AuthState.startupError();            // 토큰 유지, "다시 시도" → ref.invalidateSelf()
  }
}
```

- 로그인: `client.signIn()`이 `null`이면 상태를 바꾸지 않는다(창 닫음). id_token이 있으면 `authApi.login` → 저장 → `fromStatus`. 실패하면 `LoginFailure(code)`를 화면에 전달하고, 화면이 code별 문구를 고른다.
- 로그아웃: `authApi.logout`은 `try`로 감싸 결과를 무시한다 → `storage.clear()` → 모든 소셜 클라이언트 `signOut()`(실패 무시) → `signedOut`.
- 온보딩 건너뛰기: 상태만 `active`로 바꾼다. 서버 status는 그대로이므로, 앱을 다시 켜면 다시 자리 화면이 나온다 (온보딩 작업에서 실제 전환).

### 소셜 SDK

```dart
// 카카오: 카카오톡이 있으면 앱 로그인, 없으면(시뮬레이터) 카카오계정 웹 로그인
final token = await isKakaoTalkInstalled()
    ? await UserApi.instance.loginWithKakaoTalk()
    : await UserApi.instance.loginWithKakaoAccount();
return token.idToken ?? (throw SocialLoginException('id_token 없음: 카카오 콘솔에서 OpenID Connect 확인'));
// 사용자가 취소하면 (PlatformException 'CANCELED' 또는 access_denied) → null

// 구글: google_sign_in 최신 안정판 API로 로그인한 계정의 idToken. 취소 → null
// 개발용: DEV_LOGIN_TOKEN(기본 'simulator-dev')을 그대로 반환 → provider DEV.
//        고정값이라 같은 개발용 계정으로 계속 로그인된다
```

### 라우팅

```dart
redirect: (context, state) {
  final auth = ref.read(authControllerProvider);
  final target = switch (auth) {
    AsyncLoading() => '/splash',
    AsyncData(value: SignedOut()) => '/login',
    AsyncData(value: Onboarding()) => '/onboarding',
    AsyncData(value: StartupError()) || AsyncError() => '/startup-error',
    AsyncData(value: Active()) => null,               // 탭 화면 그대로 (/today, /me ...)
  };
  return target == null && _isAuthRoute(state.matchedLocation) ? '/today' : target;
},
refreshListenable: authStateListenable,               // 상태가 바뀌면 redirect 재평가
```

### CI (`app` 작업)

```yaml
changes:            # 바뀐 경로 판별 (dorny/paths-filter, 버전 고정)
  outputs: { app: ${{ steps.f.outputs.app }} }
app:
  needs: changes
  if: needs.changes.outputs.app == 'true'
  runs-on: ubuntu-latest   # analyze·test는 iOS 빌드가 아니라 Linux로 충분
  defaults: { run: { working-directory: app } }
  steps: checkout → subosito/flutter-action (app/pubspec.yaml의 Flutter 버전) → flutter pub get → flutter analyze → flutter test
```

- 서버 `test` 작업은 경로 필터를 걸지 않고 그대로 둔다. `main` 보호 규칙의 필수 체크가 `test`라서, 필터로 건너뛰는 방식을 섞으면 PR마다 체크 상태가 달라질 위험이 있다. 서버 테스트가 앱 PR에서도 돌지만 결과는 같다.
- `app`은 지금 필수 체크로 두지 않는다. 처음 몇 번 초록인 것을 확인한 뒤 보호 규칙에 추가할지 정한다 (사용자가 GitHub에서 설정).

## 정합성·동시성·멱등성

- **동시 갱신:** 위 `TokenRefresher` 참고. 갱신이 두 번 나가면 두 번째가 `INVALID_REFRESH_TOKEN`이 되어 정상 사용자가 로그아웃된다. 진행 중인 `Future`를 공유해서 막는다. 테스트 2번으로 확인한다.
- **갱신 중 이미 갱신된 경우:** 요청 A가 만료로 실패한 시점에 B의 갱신이 이미 끝났을 수 있다. 이때 A가 새로 갱신을 시작하면, 이미 교체된 refresh가 아니라 저장소의 최신 refresh를 읽어 보낸다(`_doRefresh`가 매번 저장소에서 읽음). 그래서 실패하지 않는다. 다만 갱신이 한 번 더 나갈 수는 있다. 보낸 access와 저장된 access를 비교해, 다르면 갱신 없이 저장된 값으로 바로 재시도하도록 한다 (테스트 3번).
- **로그아웃 중복:** 서버가 멱등(이미 없어도 204)이라 앱은 따로 처리하지 않는다.
- **서버 메모리 상태 원칙:** 해당 없음. `_inFlight`는 앱 프로세스 안의 값이다.

## 실패·대체 경로

| 실패 | 동작 |
| --- | --- |
| 소셜 창 닫음 | `null` → 아무 표시 없이 로그인 화면 유지 |
| 소셜 SDK 오류 / `INVALID_SOCIAL_TOKEN` | "로그인하지 못했어요. 다시 시도해 주세요" |
| `SOCIAL_LOGIN_UNAVAILABLE` (503) | "잠시 후 다시 시도해 주세요" |
| `UNSUPPORTED_PROVIDER` | "지금은 이 방법으로 로그인할 수 없어요" |
| 연결 실패·타임아웃 (연결 5초, 응답 10초) | "연결을 확인해 주세요". 앱 시작 중이면 다시 시도 화면, 토큰 유지 |
| 갱신 실패·`UNAUTHORIZED` | 토큰 삭제 → 로그인 화면 |
| 500·형식이 다른 응답 | "문제가 생겼어요. 잠시 후 다시 시도해 주세요" |
| 로그아웃 요청 실패 | 무시하고 기기 토큰 삭제 → 로그인 화면 |

## 테스트 목록

규칙: `test('한글 문장', ...)`, 본문은 `// given` `// when` `// then` 3구간, 가짜 객체는 mocktail(Mockk와 같은 역할). 서버 HTTP는 가짜 Dio 어댑터, 소셜 SDK는 가짜 `SocialLoginClient`, Keychain은 메모리 저장소로 바꾼다.

**단위: 토큰 갱신 (`auth_interceptor_test.dart`, `token_refresher_test.dart`)**
- [ ] 1. `401 TOKEN_EXPIRED를 받으면 갱신한 뒤 새 access로 원래 요청을 다시 보낸다`
- [ ] 2. `동시에 세 요청이 TOKEN_EXPIRED를 받아도 갱신 요청은 한 번만 보낸다`
- [ ] 3. `다른 요청이 이미 갱신했으면 갱신 없이 저장된 새 access로 다시 보낸다`
- [ ] 4. `갱신이 INVALID_REFRESH_TOKEN으로 실패하면 토큰을 지우고 세션 만료를 알린다`
- [ ] 5. `401 UNAUTHORIZED를 받으면 갱신하지 않고 토큰을 지운다`
- [ ] 6. `갱신 후 다시 보낸 요청도 401이면 다시 갱신하지 않는다`
- [ ] 7. `갱신 중 네트워크가 끊기면 토큰을 지우지 않는다`

**단위: 에러 해석 (`api_exception_test.dart`)**
- [ ] 8. `ProblemDetail 응답이면 status와 code를 읽어 ApiException으로 바꾼다`
- [ ] 9. `code가 없는 응답이면 UnknownApiException으로 바꾼다`
- [ ] 10. `연결 실패와 타임아웃은 NetworkException으로 바꾼다`

**단위: 인증 상태 (`auth_controller_test.dart`)**
- [ ] 11. `저장된 refresh가 없으면 시작 상태는 로그아웃이다`
- [ ] 12. `저장된 토큰으로 내 정보가 ACTIVE면 시작 상태는 active다`
- [ ] 13. `저장된 토큰으로 내 정보가 ONBOARDING이면 시작 상태는 onboarding이다`
- [ ] 14. `앱 시작 중 네트워크가 끊기면 토큰을 유지하고 시작 오류 상태가 된다`
- [ ] 15. `로그인에 성공하면 토큰 4개를 저장한다`
- [ ] 16. `소셜 로그인 창을 닫으면 서버를 호출하지 않고 로그아웃 상태를 유지한다`
- [ ] 17. `로그아웃 요청이 실패해도 기기 토큰을 지우고 소셜 SDK에서도 로그아웃한다`

**위젯: 화면 (`login_screen_test.dart`, `router_test.dart`, `me_screen_test.dart`)**
- [ ] 18. `로그인 화면에 카카오와 구글 버튼이 보인다`
- [ ] 19. `개발용 로그인이 꺼져 있으면 개발용 버튼이 보이지 않는다`
- [ ] 20. `SOCIAL_LOGIN_UNAVAILABLE이면 잠시 후 다시 시도하라는 문구를 보여준다`
- [ ] 21. `INVALID_SOCIAL_TOKEN이면 로그인하지 못했다는 문구를 보여준다`
- [ ] 22. `ONBOARDING 회원이 로그인하면 온보딩 준비 중 화면으로 간다`
- [ ] 23. `온보딩 준비 중 화면에서 건너뛰기를 누르면 오늘 탭으로 간다`
- [ ] 24. `ACTIVE 회원이 로그인하면 오늘 탭으로 간다`
- [ ] 25. `나 탭에 닉네임, 가입일, 연결된 소셜 종류를 보여준다`
- [ ] 26. `나 탭에서 로그아웃하면 로그인 화면으로 간다`
- [ ] 27. `세션이 만료되면 어느 탭에 있든 로그인 화면으로 간다`

**수동 확인 (iOS 시뮬레이터, 로컬 서버)**
- [ ] 개발용 로그인 → 온보딩 자리 → 건너뛰기 → 탭 5개 → 나 탭 내 정보 → 로그아웃
- [ ] 앱을 다시 켜면 로그인 화면을 건너뜀
- [ ] 화면 스크린샷을 레퍼런스(`docs/design/reference-dark-lime.webp`)와 비교
- [ ] (키가 준비되면) 카카오·구글 실제 로그인. 준비 전이면 이 항목은 "로그인 E2E" 단계로 넘긴다

## 작업 순서

0. (사용자) Flutter SDK와 Xcode 설치. `flutter doctor`의 iOS 항목이 통과해야 한다
1. `main` 최신화 → `feat/app-skeleton` 브랜치 생성. 명세·디자인 파일·계획 문서를 첫 커밋으로
2. `flutter create`, 번들 ID 수정, 의존성 추가, analysis_options, gitignore, 설정 example 파일
3. 훅(dart 포맷·분석, flutter test) 수정 → 이후 Dart 수정부터 훅이 확인
4. 테스트 1~27 작성 (시그니처만 만든 상태에서 실패 확인) → 사용자에게 보고
5. 구현: core(network, storage, config) → auth → router/shell → me → theme·widgets·화면
6. Pretendard 글꼴 추가 (받기 전 파일 이름·출처·용량을 확인받음)
7. 시뮬레이터 수동 확인과 스크린샷 비교, 디자인 다듬기
8. CI `app` 작업, CLAUDE.md 앱 섹션, 명세 수정
9. `/review-change` → 반영 → ai-log 기록 → push·PR은 사용자

## 확인 필요

- **ATS(http 허용) 범위:** 명세에는 "개발 빌드에서만 localhost에 한해 http 허용"이라고 적었다. 계획에서는 Info.plist에 `NSAllowsLocalNetworking`(로컬 주소에서만 http 허용)을 넣는다. 빌드 종류별로 Info.plist를 나누는 건 복잡해서다. 출시 빌드에도 들어가지만, 출시 빌드는 https 서버를 쓰므로 실제 효과는 없다. 이대로 명세 문구를 바꿔도 되는지 확인이 필요하다.
- **CI 경로 분리:** 명세에는 "서로의 변경에 돌지 않도록"이라고 적었다. 계획에서는 앱 작업만 경로로 걸러내고, 서버 `test`는 필수 체크라서 항상 돌게 둔다. 이대로 명세를 바꿔도 되는지 확인이 필요하다.

## 변경 이력
- 2026-11-12: "확인 필요" 두 가지(ATS는 `NSAllowsLocalNetworking`, CI는 앱 작업만 경로로 거름)를 계획대로 승인. 명세 문구를 이에 맞춰 고친다
- 2026-11-12 (구현 중): 토큰 보관소(`token_storage.dart`)와 인터셉터(`auth_interceptor.dart`)를 `core/`가 아니라 `features/auth/data/`에 두었다. 둘 다 인증 기능(TokenRefresher, AuthController)에 의존해서, `core`에 두면 "`core`는 `features`에 의존하지 않는다" 규칙을 어긴다. 인증이 필요한 Dio도 `features/auth/data/auth_dio.dart`(`authDioProvider`)에 둔다
- 2026-11-12 (구현 중): 테스트 25번의 단언 형태를 고쳤다 (Finder 묶음을 레코드로 비교하면 항상 다르다고 나와서 `expect`를 셋으로 나눔). 검증 내용은 같다
- 2026-11-12 (구현 중): Flutter 3.50은 Material 위젯을 `package:material_ui`로 분리했다. 생성된 템플릿을 따라 이 패키지를 쓴다
- 2026-11-12 (리뷰 반영, 지적 11건 모두):
  1. 갱신 중 로그아웃하면 늦게 온 갱신 응답이 지운 토큰을 다시 저장하던 문제 → `TokenStorage.generation`(clear마다 증가)을 두고, 갱신 응답을 받았을 때 세대가 바뀌었으면 저장하지 않고 새 refresh를 서버에서 폐기. 테스트 추가 (`갱신 중에 로그아웃되면 늦게 온 갱신 응답의 토큰을 저장하지 않는다`)
  2. 테스트 20·21이 기본 문구에도 걸리던 것 → 문구 전체를 정확히 비교
  3. 테스트 18·19가 소셜 클라이언트 목록을 통째로 바꿔 끼워 설정을 검증하지 못하던 것 → `TestDeps.clients`가 없으면 실제 provider 사용. `개발용 로그인이 켜져 있으면 개발용 버튼이 보인다` 추가
  4. Riverpod 3 기본 자동 재시도(약 38초) → `core/network/retry_policy.dart`의 `appRetry`(AppException은 재시도 안 함)를 앱과 테스트가 함께 사용. `나 탭에서 연결이 끊기면 재시도를 기다리지 않고 다시 시도 버튼을 보여준다` 추가
  5. 테스트 2·3이 재요청 성공을 확인하지 않던 것 → 결과 단언 추가
  6. 앱 재시작 후 구글 로그아웃이 생략되던 것 → 로그아웃 전에도 SDK 초기화
  7. Pretendard 글꼴 추가 (1.3.9, SIL OFL 1.1, OTF 5종 약 7.9MB, jsDelivr의 공식 npm 패키지에서 받음)
  8. 앱 시작 중 500도 "연결을 확인해 주세요"로 보이던 것 → `StartupError(StartupFailure.network|server)`로 문구 분리. `앱 시작 중 서버 오류면 ... 문제가 생겼다는 문구를 보여준다` 추가. 나 탭 오류 문구도 연결 실패와 나머지를 나눔
  9. 가입일 테스트가 시간대 처리 누락을 잡지 못하던 것 → UTC와 한국 시간에서 날짜가 갈리는 값으로 바꾸고, 앱 테스트를 `TZ=Asia/Seoul`로 고정 (CI, 종료 훅, CLAUDE.md)
  10. 개발용 로그인 버튼이 출시 빌드에서도 보일 수 있던 것 → `kDebugMode`일 때만
  11. 빈 닉네임이면 나 탭이 깨지던 것 → '?'로 표시
  - 테스트는 27개 → 31개
