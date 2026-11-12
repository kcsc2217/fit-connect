import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';

/// 앱의 로그인 상태. 라우터는 이 값만 보고 화면을 정한다.
sealed class AuthState {
  const AuthState();

  static AuthState fromStatus(UserStatus status) => switch (status) {
    UserStatus.onboarding => const Onboarding(),
    UserStatus.active => const Active(),
  };
}

/// 로그인 안 됨 → 로그인 화면
class SignedOut extends AuthState {
  const SignedOut();
}

/// 로그인됨, 온보딩 전 → 온보딩 자리 화면
class Onboarding extends AuthState {
  const Onboarding();
}

/// 로그인됨 → 메인 탭
class Active extends AuthState {
  const Active();
}

/// 앱 시작 중 서버에 못 붙음 → 다시 시도 화면 (토큰은 유지)
class StartupError extends AuthState {
  const StartupError();
}
