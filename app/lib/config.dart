/// 실행 시 --dart-define-from-file=keys.json 으로 넣는 값 (keys.example.json 참고).
/// 값이 비어 있으면 그 로그인 버튼을 끈다.
class AppConfig {
  /// 안드로이드 에뮬레이터에서 10.0.2.2는 맥(호스트)의 localhost다
  static const apiBaseUrl = String.fromEnvironment('API_BASE_URL', defaultValue: 'http://10.0.2.2:8080');

  /// 카카오 개발자 콘솔 > 앱 키 > 네이티브 앱 키
  static const kakaoNativeAppKey = String.fromEnvironment('KAKAO_NATIVE_APP_KEY');

  /// 구글 클라우드 콘솔의 "웹 애플리케이션" OAuth 클라이언트 ID.
  /// id_token의 aud가 이 값이 되므로 서버 GOOGLE_CLIENT_IDS에도 같은 값을 넣는다.
  static const googleServerClientId = String.fromEnvironment('GOOGLE_SERVER_CLIENT_ID');

  static bool get kakaoEnabled => kakaoNativeAppKey.isNotEmpty;
  static bool get googleEnabled => googleServerClientId.isNotEmpty;
}
