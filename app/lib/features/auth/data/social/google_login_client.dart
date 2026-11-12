import 'package:google_sign_in/google_sign_in.dart';
import 'package:trainer_ssaem/features/auth/data/social/social_login_client.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';

/// 구글 로그인 (google_sign_in 7.x). id_token의 aud는 iOS 클라이언트 ID라서
/// 서버 GOOGLE_CLIENT_IDS에 그 값이 있어야 한다.
class GoogleLoginClient implements SocialLoginClient {
  GoogleLoginClient(this._iosClientId);

  final String _iosClientId;
  Future<void>? _initialized;

  @override
  AuthProvider get provider => AuthProvider.google;

  /// SDK 초기화는 처음 쓸 때 한 번만 한다
  Future<void> _ensureInitialized() =>
      _initialized ??= GoogleSignIn.instance.initialize(clientId: _iosClientId);

  @override
  Future<String?> signIn() async {
    if (_iosClientId.isEmpty) {
      throw const SocialLoginException('GOOGLE_IOS_CLIENT_ID 설정이 없음');
    }
    try {
      await _ensureInitialized();
      final account = await GoogleSignIn.instance.authenticate();
      final idToken = account.authentication.idToken;
      if (idToken == null) {
        throw const SocialLoginException('구글 id_token 없음');
      }
      return idToken;
    } on GoogleSignInException catch (e) {
      if (e.code == GoogleSignInExceptionCode.canceled) return null;
      throw SocialLoginException('구글 로그인 실패: ${e.code.name}');
    }
  }

  @override
  Future<void> signOut() async {
    if (_iosClientId.isEmpty || _initialized == null) return;
    try {
      await GoogleSignIn.instance.signOut();
    } on Object {
      // 로그인 상태가 아니면 무시
    }
  }
}
