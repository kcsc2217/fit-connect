import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:trainer_ssaem/core/network/api_exception.dart';
import 'package:trainer_ssaem/features/auth/data/auth_api.dart';
import 'package:trainer_ssaem/features/auth/data/social/social_login_client.dart';
import 'package:trainer_ssaem/features/auth/data/token_storage.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_state.dart';
import 'package:trainer_ssaem/features/me/data/me_api.dart';

/// 로그인 상태를 관리한다: 앱 시작 판정, 로그인, 로그아웃, 세션 만료, 온보딩 건너뛰기.
class AuthController extends AsyncNotifier<AuthState> {
  TokenStorage get _storage => ref.read(tokenStorageProvider);

  /// 앱 시작 판정. 저장된 refresh가 없으면 로그아웃, 있으면 내 정보로 상태를 정한다.
  @override
  Future<AuthState> build() async {
    if (await _storage.readRefreshToken() == null) return const SignedOut();
    try {
      final me = await ref.read(meApiProvider).fetch(); // 만료면 인터셉터가 갱신
      return AuthState.fromStatus(me.status);
    } on SessionExpiredException {
      return const SignedOut(); // 토큰은 인터셉터가 이미 지웠다
    } on AppException {
      // 연결 실패, 서버 오류: 토큰은 두고 다시 시도하게 한다
      return const StartupError();
    }
  }

  /// 소셜 로그인 → 서버 로그인 → 토큰 저장.
  /// 사용자가 창을 닫으면 아무것도 바꾸지 않는다.
  /// 실패하면 AppException 또는 SocialLoginException을 던진다 (화면이 문구를 고른다).
  Future<void> signIn(AuthProvider provider) async {
    final client = ref.read(socialLoginClientsProvider)[provider];
    if (client == null) {
      throw SocialLoginException('${provider.wire} 클라이언트가 없음');
    }
    final idToken = await client.signIn();
    if (idToken == null) return; // 창을 닫음

    final result = await ref.read(authApiProvider).login(provider, idToken);
    await _storage.save(result.tokens);
    state = AsyncData(AuthState.fromStatus(result.status));
  }

  /// 서버 로그아웃(실패 무시) → 기기 토큰 삭제 → 소셜 SDK 로그아웃 → 로그인 화면.
  Future<void> signOut() async {
    final refreshToken = await _storage.readRefreshToken();
    if (refreshToken != null) {
      try {
        await ref.read(authApiProvider).logout(refreshToken);
      } on AppException {
        // 서버 쪽 refresh는 14일 뒤 만료된다. 기기에서는 지운다
      }
    }
    await _storage.clear();
    for (final client in ref.read(socialLoginClientsProvider).values) {
      try {
        await client.signOut();
      } on Object {
        // SDK 로그아웃 실패는 무시한다 (다음 로그인 때 계정 선택이 생략될 뿐)
      }
    }
    state = const AsyncData(SignedOut());
  }

  /// 인터셉터가 부른다 (토큰은 이미 지워짐).
  void sessionExpired() {
    if (state.value is! SignedOut) state = const AsyncData(SignedOut());
  }

  /// 온보딩 자리 화면의 "건너뛰기". 서버 상태는 바꾸지 않는다 (앱을 다시 켜면 다시 보인다).
  void skipOnboarding() {
    if (state.value is Onboarding) state = const AsyncData(Active());
  }

  /// 시작 오류 화면의 "다시 시도".
  void retryStartup() => ref.invalidateSelf();
}

final authControllerProvider = AsyncNotifierProvider<AuthController, AuthState>(
  AuthController.new,
);
