import 'package:google_sign_in/google_sign_in.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';

import '../config.dart';

class SocialLoginException implements Exception {
  const SocialLoginException(this.message);

  final String message;

  @override
  String toString() => message;
}

/// 앱 시작 시 한 번 호출한다
Future<void> initSocialSdks() async {
  if (AppConfig.kakaoEnabled) {
    KakaoSdk.init(nativeAppKey: AppConfig.kakaoNativeAppKey);
  }
  if (AppConfig.googleEnabled) {
    // serverClientId(웹 클라이언트 ID)를 주면 id_token의 aud가 플랫폼과 관계없이 이 값 하나가 된다.
    // 그래서 서버 허용 목록(GOOGLE_CLIENT_IDS)에는 웹 클라이언트 ID 하나만 넣으면 된다.
    await GoogleSignIn.instance.initialize(serverClientId: AppConfig.googleServerClientId);
  }
}

/// 카카오 로그인 후 OIDC id_token을 돌려준다.
/// 에뮬레이터에는 카카오톡이 없어 카카오계정(웹) 로그인으로 간다.
Future<String> kakaoIdToken() async {
  final OAuthToken token = await isKakaoTalkInstalled()
      ? await UserApi.instance.loginWithKakaoTalk()
      : await UserApi.instance.loginWithKakaoAccount();
  final idToken = token.idToken;
  // 콘솔에서 OpenID Connect를 켜지 않으면 로그인은 성공하고 id_token만 비어 온다.
  // 서버 401과 헷갈리지 않게 여기서 원인을 알려 준다.
  if (idToken == null) {
    throw const SocialLoginException('카카오 id_token이 없습니다. 카카오 콘솔에서 OpenID Connect를 켜세요.');
  }
  return idToken;
}

/// 구글 로그인 후 id_token을 돌려준다
Future<String> googleIdToken() async {
  final account = await GoogleSignIn.instance.authenticate();
  final idToken = account.authentication.idToken;
  if (idToken == null) {
    throw const SocialLoginException('구글 id_token을 받지 못했습니다. serverClientId(웹 클라이언트 ID)를 확인하세요.');
  }
  return idToken;
}
