import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/theme/app_colors.dart';
import 'package:trainer_ssaem/core/widgets/app_logo.dart';
import 'package:trainer_ssaem/core/widgets/primary_button.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_controller.dart';

/// 앱 시작 판정 중 (저장된 토큰 확인, 내 정보 조회)
class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const Scaffold(body: Center(child: AppLogo(size: 72)));
  }
}

/// 앱 시작 중 서버에 못 붙음. 토큰은 지우지 않았으므로 다시 시도하면 된다.
class StartupErrorScreen extends ConsumerWidget {
  const StartupErrorScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final text = Theme.of(context).textTheme;
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 32, 24, 20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Spacer(),
              const Icon(
                Icons.wifi_off_rounded,
                size: 48,
                color: AppColors.textSecondary,
              ),
              const SizedBox(height: 20),
              Text('연결을 확인해 주세요', style: text.headlineSmall),
              const SizedBox(height: 12),
              Text(
                '서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.',
                style: text.bodyLarge?.copyWith(color: AppColors.textSecondary),
              ),
              const Spacer(flex: 2),
              PrimaryButton(
                label: '다시 시도',
                onPressed: () =>
                    ref.read(authControllerProvider.notifier).retryStartup(),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
