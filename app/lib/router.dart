import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_controller.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_state.dart';
import 'package:trainer_ssaem/features/auth/presentation/login_screen.dart';
import 'package:trainer_ssaem/features/auth/presentation/onboarding_placeholder_screen.dart';
import 'package:trainer_ssaem/features/auth/presentation/startup_screens.dart';
import 'package:trainer_ssaem/features/me/presentation/me_screen.dart';
import 'package:trainer_ssaem/features/shell/main_shell.dart';

abstract final class Routes {
  static const splash = '/splash';
  static const login = '/login';
  static const onboarding = '/onboarding';
  static const startupError = '/startup-error';
  static const today = '/today';
  static const diet = '/diet';
  static const coach = '/coach';
  static const challenge = '/challenge';
  static const me = '/me';

  /// 로그인 전·온보딩·오류 화면 (탭 밖)
  static const outsideTabs = {splash, login, onboarding, startupError};
}

/// 로그인 상태로 갈 곳을 정한다. null이면 지금 화면 그대로.
String? redirectFor(AsyncValue<AuthState> auth, String location) {
  final String target;
  if (auth.isLoading) {
    target = Routes.splash;
  } else {
    target = switch (auth.value) {
      SignedOut() => Routes.login,
      Onboarding() => Routes.onboarding,
      Active() =>
        Routes.outsideTabs.contains(location) ? Routes.today : location,
      StartupError() || null => Routes.startupError, // 예상하지 못한 오류 포함
    };
  }
  return target == location ? null : target;
}

/// 화면 경로와, 로그인 상태에 따른 이동 규칙.
final routerProvider = Provider<GoRouter>((ref) {
  // 로그인 상태가 바뀌면 redirect를 다시 계산하게 한다
  final authChanges = ValueNotifier<int>(0);
  ref.listen(authControllerProvider, (_, _) => authChanges.value++);

  GoRoute tab(String path, Widget child) =>
      GoRoute(path: path, builder: (context, state) => child);

  final router = GoRouter(
    initialLocation: Routes.splash,
    refreshListenable: authChanges,
    redirect: (context, state) =>
        redirectFor(ref.read(authControllerProvider), state.matchedLocation),
    routes: [
      GoRoute(
        path: Routes.splash,
        builder: (context, state) => const SplashScreen(),
      ),
      GoRoute(
        path: Routes.login,
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        path: Routes.onboarding,
        builder: (context, state) => const OnboardingPlaceholderScreen(),
      ),
      GoRoute(
        path: Routes.startupError,
        builder: (context, state) => const StartupErrorScreen(),
      ),
      StatefulShellRoute.indexedStack(
        builder: (context, state, shell) => MainShell(navigationShell: shell),
        branches: [
          StatefulShellBranch(routes: [tab(Routes.today, const TodayTab())]),
          StatefulShellBranch(routes: [tab(Routes.diet, const DietTab())]),
          StatefulShellBranch(routes: [tab(Routes.coach, const CoachTab())]),
          StatefulShellBranch(
            routes: [tab(Routes.challenge, const ChallengeTab())],
          ),
          StatefulShellBranch(routes: [tab(Routes.me, const MeScreen())]),
        ],
      ),
    ],
  );

  ref.onDispose(() {
    router.dispose();
    authChanges.dispose();
  });
  return router;
});
