import 'package:mocktail/mocktail.dart';
import 'package:trainer_ssaem/features/auth/data/social/social_login_client.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';

/// 소셜 SDK 대신 쓰는 가짜 (mocktail, Mockk의 `mockk<T>()`와 같은 역할).
/// signIn·signOut의 동작은 테스트마다 when(...)으로 정한다.
class FakeSocialLoginClient extends Mock implements SocialLoginClient {
  FakeSocialLoginClient(this._provider, {String? idToken}) {
    when(signIn).thenAnswer((_) async => idToken);
    when(signOut).thenAnswer((_) async {});
  }

  final AuthProvider _provider;

  @override
  AuthProvider get provider => _provider;
}
