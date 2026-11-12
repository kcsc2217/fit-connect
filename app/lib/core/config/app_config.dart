import 'package:flutter_riverpod/flutter_riverpod.dart';

/// 빌드할 때 넘기는 설정 값 (`--dart-define-from-file=config/local.json`).
/// 값은 코드에 넣지 않는다. 예시는 `config/example.json`.
class AppConfig {
  const AppConfig({
    required this.apiBaseUrl,
    this.kakaoNativeAppKey = '',
    this.googleIosClientId = '',
    this.devLoginEnabled = false,
    this.devLoginToken = 'simulator-dev',
  });

  factory AppConfig.fromEnvironment() => const AppConfig(
    apiBaseUrl: String.fromEnvironment(
      'API_BASE_URL',
      defaultValue: 'http://localhost:8080',
    ),
    kakaoNativeAppKey: String.fromEnvironment('KAKAO_NATIVE_APP_KEY'),
    googleIosClientId: String.fromEnvironment('GOOGLE_IOS_CLIENT_ID'),
    devLoginEnabled: bool.fromEnvironment('DEV_LOGIN_ENABLED'),
    devLoginToken: String.fromEnvironment(
      'DEV_LOGIN_TOKEN',
      defaultValue: 'simulator-dev',
    ),
  );

  /// 서버 주소. 시뮬레이터는 localhost로 Mac의 서버에 붙는다.
  final String apiBaseUrl;

  /// 카카오 네이티브 앱 키. 서버의 KAKAO_APP_KEYS와 짝이 맞아야 한다.
  final String kakaoNativeAppKey;

  /// 구글 iOS 클라이언트 ID. 서버의 GOOGLE_CLIENT_IDS에 들어 있어야 한다.
  final String googleIosClientId;

  /// 개발용 로그인 버튼 표시 여부. 서버도 로컬 설정에서만 DEV를 받는다.
  final bool devLoginEnabled;

  /// 개발용 로그인에 보내는 고정값. 같은 값이면 같은 개발용 계정이 된다.
  final String devLoginToken;
}

final appConfigProvider = Provider<AppConfig>(
  (ref) => AppConfig.fromEnvironment(),
);
