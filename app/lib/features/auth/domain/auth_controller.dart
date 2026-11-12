import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_state.dart';

/// 로그인 상태를 관리한다: 앱 시작 판정, 로그인, 로그아웃, 세션 만료, 온보딩 건너뛰기.
class AuthController extends AsyncNotifier<AuthState> {
  /// 앱 시작 판정. 저장된 refresh가 없으면 로그아웃, 있으면 내 정보로 상태를 정한다.
  @override
  Future<AuthState> build() => throw UnimplementedError();

  /// 소셜 로그인 → 서버 로그인 → 토큰 저장.
  /// 사용자가 창을 닫으면 아무것도 바꾸지 않는다.
  /// 실패하면 AppException 또는 SocialLoginException을 던진다 (화면이 문구를 고른다).
  Future<void> signIn(AuthProvider provider) => throw UnimplementedError();

  /// 서버 로그아웃(실패 무시) → 기기 토큰 삭제 → 소셜 SDK 로그아웃 → 로그인 화면.
  Future<void> signOut() => throw UnimplementedError();

  /// 인터셉터가 부른다 (토큰은 이미 지워짐).
  void sessionExpired() => throw UnimplementedError();

  /// 온보딩 자리 화면의 "건너뛰기". 서버 상태는 바꾸지 않는다.
  void skipOnboarding() => throw UnimplementedError();

  /// 시작 오류 화면의 "다시 시도".
  void retryStartup() => throw UnimplementedError();
}

final authControllerProvider = AsyncNotifierProvider<AuthController, AuthState>(
  AuthController.new,
);
