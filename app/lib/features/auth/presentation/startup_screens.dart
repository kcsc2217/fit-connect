import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/theme/app_colors.dart';
import 'package:trainer_ssaem/core/widgets/app_logo.dart';
import 'package:trainer_ssaem/core/widgets/primary_button.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_controller.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_state.dart';

/// 앱 시작 판정 중 (저장된 토큰 확인, 내 정보 조회)
class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const Scaffold(body: Center(child: AppLogo(size: 72)));
  }
}

/// 앱 시작 중 내 정보를 못 받음. 토큰은 지우지 않았으므로 다시 시도하면 된다.
class StartupErrorScreen extends ConsumerWidget {
  const StartupErrorScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final text = Theme.of(context).textTheme;
    final auth = ref.watch(authControllerProvider).value;
    // 연결 실패만 "연결 확인", 그 밖(서버 오류, 예상하지 못한 오류)은 "문제가 생겼어요" (명세 예외 상황 표)
    final network =
        auth is StartupError && auth.failure == StartupFailure.network;
    final (icon, title, body) = network
        ? (
            Icons.wifi_off_rounded,
            '연결을 확인해 주세요',
            '서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.',
          )
        : (Icons.error_outline_rounded, '문제가 생겼어요', '잠시 후 다시 시도해 주세요.');
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 32, 24, 20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Spacer(),
              Icon(icon, size: 48, color: AppColors.textSecondary),
              const SizedBox(height: 20),
              Text(title, style: text.headlineSmall),
              const SizedBox(height: 12),
              Text(
                body,
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
