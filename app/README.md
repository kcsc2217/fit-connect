# 로그인 확인용 앱 (Flutter)

백엔드 인증(계획 006·007)을 실제 기기 흐름으로 확인하는 **한 화면짜리 앱**이다. 계획은 `docs/plans/008-login-test-app.md`.

DEV·카카오·구글 로그인 → `POST /api/v1/auth/login` → `GET /api/v1/users/me` 결과 표시. 소셜 로그인 때는 받은 id_token의 `iss`·`aud`도 보여 준다 (서버 허용 목록에 넣을 값 확인용).

## 준비 (한 번)

1. Flutter SDK: `brew install --cask flutter`
2. Android Studio: `brew install --cask android-studio` → 처음 열어 설정 마법사를 기본값으로 끝까지 진행 (SDK 설치) → **Device Manager**에서 에뮬레이터(Google Play 포함 이미지) 하나 만들기
3. `flutter doctor --android-licenses` (라이선스 동의)
4. 키 파일: `cp keys.example.json keys.json` 후 값 채우기 (`keys.json`은 커밋하지 않는다)

| 키 | 값 | 비어 있으면 |
| --- | --- | --- |
| `API_BASE_URL` | 에뮬레이터에서는 `http://10.0.2.2:8080` (맥의 localhost) | 기본값 사용 |
| `KAKAO_NATIVE_APP_KEY` | 카카오 콘솔 > 앱 키 > 네이티브 앱 키 | 카카오 버튼 꺼짐 |
| `GOOGLE_SERVER_CLIENT_ID` | 구글 콘솔의 **웹 애플리케이션** OAuth 클라이언트 ID | 구글 버튼 꺼짐 |

## 실행

```bash
# 백엔드 (저장소 루트에서, local 프로파일이면 DEV 로그인이 켜져 있다)
./gradlew bootRun

# 앱 (app/에서, 에뮬레이터를 켠 뒤)
flutter run --dart-define-from-file=keys.json
```

테스트: `flutter test` (서버 없이 대역으로 돈다)

## 콘솔 등록

### 카카오 (developers.kakao.com)

1. 내 애플리케이션 > 애플리케이션 추가
2. 앱 > 플랫폼 > Android 등록: 패키지명 `com.fitness.traffic_gym_app`, 키 해시는 디버그 키스토어 값 (아래 명령)
3. 카카오 로그인 > 활성화 ON, **OpenID Connect 활성화 ON** (끄면 id_token이 오지 않는다)
4. 앱 키 > 네이티브 앱 키를 `keys.json`의 `KAKAO_NATIVE_APP_KEY`에

```bash
# 디버그 키 해시 (첫 빌드 후 ~/.android/debug.keystore가 생긴다)
keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android | openssl sha1 -binary | openssl base64
```

### 구글 (console.cloud.google.com)

1. 프로젝트 만들기 > API 및 서비스 > OAuth 동의 화면 설정 (외부, 테스트 사용자에 본인 계정 추가)
2. 사용자 인증 정보 > OAuth 클라이언트 ID를 **두 개** 만든다
   - **Android**: 패키지명 `com.fitness.traffic_gym_app`, SHA-1은 디버그 키스토어 값 (아래 명령). 앱이 직접 쓰지는 않지만 없으면 로그인이 실패한다
   - **웹 애플리케이션**: 이 ID를 `keys.json`의 `GOOGLE_SERVER_CLIENT_ID`에. id_token의 `aud`가 이 값이 된다
3. 에뮬레이터는 Google Play가 포함된 이미지여야 하고, 기기에 구글 계정이 로그인돼 있어야 한다

```bash
keytool -list -v -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android | grep SHA1
```

### 서버 허용 aud

로그인 후 화면에 나온 `aud`를 백엔드 `application-local.yml`에 넣고 서버를 다시 띄운다.

```yaml
fitness:
  auth:
    kakao:
      audiences: <카카오 aud>
    google:
      audiences: <구글 aud = 웹 클라이언트 ID>
```

운영은 환경변수 `KAKAO_APP_KEYS`, `GOOGLE_CLIENT_IDS`. 확인 결과는 `docs/auth-e2e-check.md`.

## 알아둘 것

- 토큰은 `flutter_secure_storage`(안드로이드 Keystore 암호화)에 둔다. 앱을 다시 열면 저장된 토큰으로 내 정보를 바로 불러온다.
- 401 `TOKEN_EXPIRED`일 때만 refresh를 한 번 시도한다. refresh는 쓸 때마다 바뀌어 받은 즉시 저장한다.
- iOS 설정은 아직 없다 (이 맥은 macOS 15.2라 최신 Xcode 설치 불가). Dart 코드는 공용이라 Info.plist URL 스킴과 구글 iOS 클라이언트 ID만 붙이면 된다.
