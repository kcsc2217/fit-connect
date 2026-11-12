import 'package:flutter/services.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:trainer_ssaem/features/auth/data/social/social_login_client.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';

/// 카카오 SDK 로그인. 카카오톡이 있으면 앱으로, 없으면(시뮬레이터) 카카오계정 웹 로그인.
/// 콘솔에서 OpenID Connect를 켜야 id_token이 나온다. SDK 초기화는 main()에서 한다.
class KakaoLoginClient implements SocialLoginClient {
  KakaoLoginClient(this._nativeAppKey);

  final String _nativeAppKey;

  @override
  AuthProvider get provider => AuthProvider.kakao;

  @override
  Future<String?> signIn() async {
    if (_nativeAppKey.isEmpty) {
      throw const SocialLoginException('KAKAO_NATIVE_APP_KEY 설정이 없음');
    }
    final OAuthToken token;
    try {
      token = await isKakaoTalkInstalled()
          ? await UserApi.instance.loginWithKakaoTalk()
          : await UserApi.instance.loginWithKakaoAccount();
    } on PlatformException catch (e) {
      // 카카오톡 로그인 화면에서 취소
      if (e.code == 'CANCELED') return null;
      throw SocialLoginException('카카오 로그인 실패: ${e.code}');
    } on KakaoAuthException catch (e) {
      // 카카오계정 웹 로그인에서 취소·동의 거부
      if (e.error == AuthErrorCause.accessDenied) return null;
      throw SocialLoginException('카카오 로그인 실패: ${e.error}');
    } on KakaoException catch (e) {
      throw SocialLoginException('카카오 로그인 실패: ${e.runtimeType}');
    }
    final idToken = token.idToken;
    if (idToken == null) {
      throw const SocialLoginException(
        'id_token 없음: 카카오 콘솔에서 OpenID Connect가 켜져 있는지 확인',
      );
    }
    return idToken;
  }

  @override
  Future<void> signOut() async {
    if (_nativeAppKey.isEmpty) return;
    try {
      await UserApi.instance.logout();
    } on Object {
      // 카카오 로그인을 안 했거나 토큰이 이미 없음
    }
  }
}
