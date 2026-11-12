import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:trainer_ssaem/core/config/app_config.dart';
import 'package:trainer_ssaem/features/auth/data/social/dev_login_client.dart';
import 'package:trainer_ssaem/features/auth/data/social/google_login_client.dart';
import 'package:trainer_ssaem/features/auth/data/social/kakao_login_client.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';

/// 소셜 SDK 로그인을 감싼다. 테스트에서는 가짜로 바꿔 끼운다.
abstract interface class SocialLoginClient {
  AuthProvider get provider;

  /// 로그인해 id_token을 돌려준다. 사용자가 창을 닫으면 null.
  /// SDK 오류는 [SocialLoginException].
  Future<String?> signIn();

  /// SDK의 로그인 상태를 끊는다 (다음에 계정을 다시 고르게).
  Future<void> signOut();
}

class SocialLoginException implements Exception {
  const SocialLoginException(this.message);

  /// 개발자용 원인 (화면에는 보이지 않는다)
  final String message;

  @override
  String toString() => 'SocialLoginException($message)';
}

/// 로그인 화면에 보일 클라이언트. 개발용은 설정이 켜졌을 때만 들어간다.
final socialLoginClientsProvider =
    Provider<Map<AuthProvider, SocialLoginClient>>((ref) {
      final config = ref.watch(appConfigProvider);
      return {
        AuthProvider.kakao: KakaoLoginClient(),
        AuthProvider.google: GoogleLoginClient(config.googleIosClientId),
        if (config.devLoginEnabled)
          AuthProvider.dev: DevLoginClient(config.devLoginToken),
      };
    });
