# 트레이너쌤 앱 (Flutter)

Traffic-Gym 서버를 쓰는 모바일 앱. 규칙과 구조는 저장소 루트 `CLAUDE.md`의 "앱" 섹션을 따른다.

## 처음 실행

1. Flutter 3.50.0, Xcode(iOS 시뮬레이터) 설치. `flutter doctor`의 iOS 항목이 통과해야 한다.
2. 설정 파일 만들기 (커밋하지 않는다)
   ```bash
   cp config/example.json config/local.json
   cp ios/Flutter/Secrets.example.xcconfig ios/Flutter/Secrets.xcconfig
   ```
   키 없이도 개발용 로그인(`DEV_LOGIN_ENABLED: "true"`)으로 흐름을 확인할 수 있다.
3. 로컬 서버 실행 (저장소 루트에서 `./gradlew bootRun`, 8080)
4. 실행
   ```bash
   flutter run --dart-define-from-file=config/local.json
   ```

## 실제 소셜 로그인을 켜려면

- 카카오: 개발자 콘솔에서 iOS 번들 ID `com.fitness.trainerssaem` 등록, OpenID Connect 켜기. 네이티브 앱 키를 `config/local.json`과 `Secrets.xcconfig`에 넣고, 서버 `KAKAO_APP_KEYS`에도 같은 키를 넣는다.
- 구글: Google Cloud 콘솔에서 iOS OAuth 클라이언트 생성 (번들 ID 같음). 클라이언트 ID를 두 파일에 넣고(`Secrets.xcconfig`에는 거꾸로 쓴 URL 스킴도), 서버 `GOOGLE_CLIENT_IDS`에도 넣는다.
