import 'package:trainer_ssaem/features/auth/data/social/social_login_client.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';

/// 카카오 SDK 로그인. 카카오톡이 있으면 앱으로, 없으면(시뮬레이터) 카카오계정 웹 로그인.
/// 콘솔에서 OpenID Connect를 켜야 id_token이 나온다.
class KakaoLoginClient implements SocialLoginClient {
  @override
  AuthProvider get provider => AuthProvider.kakao;

  @override
  Future<String?> signIn() => throw UnimplementedError();

  @override
  Future<void> signOut() => throw UnimplementedError();
}
