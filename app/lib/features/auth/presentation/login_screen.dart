import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/network/api_exception.dart';
import 'package:trainer_ssaem/core/theme/app_colors.dart';
import 'package:trainer_ssaem/core/widgets/app_logo.dart';
import 'package:trainer_ssaem/core/widgets/primary_button.dart';
import 'package:trainer_ssaem/features/auth/data/social/social_login_client.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_controller.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';

/// 로그인 실패를 사용자 문구로 바꾼다. 문구가 아니라 code로 분기한다 (docs/decisions/0003).
String loginErrorMessage(Object error) => switch (error) {
  ApiException(code: 'SOCIAL_LOGIN_UNAVAILABLE') => '잠시 후 다시 시도해 주세요.',
  ApiException(code: 'UNSUPPORTED_PROVIDER') => '지금은 이 방법으로 로그인할 수 없어요.',
  ApiException(code: 'INVALID_SOCIAL_TOKEN') ||
  SocialLoginException() => '로그인하지 못했어요. 다시 시도해 주세요.',
  NetworkException() => '연결을 확인해 주세요.',
  _ => '문제가 생겼어요. 잠시 후 다시 시도해 주세요.',
};

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  /// 진행 중인 로그인 (버튼 진행 표시용)
  AuthProvider? _pending;
  String? _error;

  Future<void> _signIn(AuthProvider provider) async {
    setState(() {
      _pending = provider;
      _error = null;
    });
    try {
      await ref.read(authControllerProvider.notifier).signIn(provider);
    } on Object catch (e) {
      if (mounted) setState(() => _error = loginErrorMessage(e));
    } finally {
      if (mounted) setState(() => _pending = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final clients = ref.watch(socialLoginClientsProvider);
    final busy = _pending != null;

    Widget button(AuthProvider provider) => switch (provider) {
      AuthProvider.kakao => PrimaryButton(
        label: '카카오로 시작하기',
        leading: const Icon(Icons.chat_bubble_rounded, size: 18),
        background: AppColors.kakaoYellow,
        foreground: AppColors.kakaoLabel,
        loading: _pending == provider,
        onPressed: busy ? null : () => _signIn(provider),
      ),
      AuthProvider.google => PrimaryButton(
        label: 'Google로 시작하기',
        leading: const Text(
          'G',
          style: TextStyle(fontWeight: FontWeight.w800, fontSize: 18),
        ),
        background: AppColors.googleWhite,
        foreground: AppColors.googleLabel,
        loading: _pending == provider,
        onPressed: busy ? null : () => _signIn(provider),
      ),
      _ => PrimaryButton.outlined(
        label: '개발용 로그인',
        leading: const Icon(Icons.developer_mode_rounded, size: 18),
        loading: _pending == provider,
        onPressed: busy ? null : () => _signIn(provider),
      ),
    };

    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 32, 24, 20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Spacer(),
              const AppLogo(size: 72),
              const SizedBox(height: 28),
              Text('트레이너쌤', style: text.displaySmall),
              const SizedBox(height: 12),
              Text(
                '내 기록을 아는 트레이너가\n매일 오늘의 운동을 짜드려요.',
                style: text.bodyLarge?.copyWith(color: AppColors.textSecondary),
              ),
              const Spacer(flex: 2),
              for (final provider in [
                AuthProvider.kakao,
                AuthProvider.google,
                AuthProvider.dev,
              ])
                if (clients.containsKey(provider)) ...[
                  button(provider),
                  const SizedBox(height: 12),
                ],
              SizedBox(
                height: 44,
                child: Center(
                  child: _error == null
                      ? Text(
                          '계속하면 이용약관과 개인정보 처리방침에 동의하게 돼요.',
                          textAlign: TextAlign.center,
                          style: text.bodySmall?.copyWith(
                            color: AppColors.textTertiary,
                          ),
                        )
                      : Text(
                          _error!,
                          textAlign: TextAlign.center,
                          style: text.bodyMedium?.copyWith(
                            color: AppColors.error,
                          ),
                        ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
