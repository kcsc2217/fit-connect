import 'package:trainer_ssaem/features/auth/data/social/social_login_client.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';

/// 구글 로그인 (google_sign_in 7.x). id_token의 aud는 iOS 클라이언트 ID라서
/// 서버 GOOGLE_CLIENT_IDS에 그 값이 있어야 한다.
class GoogleLoginClient implements SocialLoginClient {
  GoogleLoginClient(this._iosClientId);

  final String _iosClientId;

  @override
  AuthProvider get provider => AuthProvider.google;

  @override
  Future<String?> signIn() => throw UnimplementedError();

  @override
  Future<void> signOut() => throw UnimplementedError();

  // 스텁 단계에서 필드 미사용 경고를 피하기 위한 접근 (구현 시 제거)
  String get clientId => _iosClientId;
}
