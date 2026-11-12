import 'package:trainer_ssaem/features/auth/data/social/social_login_client.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';

/// 개발용 로그인. 고정값을 그대로 토큰으로 보낸다 (서버 DevTokenVerifier가 받음).
/// 서버가 로컬 설정(fitness.auth.dev-login.enabled)일 때만 동작한다.
class DevLoginClient implements SocialLoginClient {
  DevLoginClient(this._token);

  final String _token;

  @override
  AuthProvider get provider => AuthProvider.dev;

  @override
  Future<String?> signIn() async => _token;

  @override
  Future<void> signOut() async {}
}
