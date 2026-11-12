import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/theme/app_colors.dart';
import 'package:trainer_ssaem/core/widgets/app_logo.dart';
import 'package:trainer_ssaem/core/widgets/primary_button.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_controller.dart';

/// status = ONBOARDING일 때 보이는 자리 화면. 온보딩 작업(FR-03)이 실제 입력 화면으로 바꾼다.
class OnboardingPlaceholderScreen extends ConsumerWidget {
  const OnboardingPlaceholderScreen({super.key});

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
              const AppLogo(size: 56),
              const SizedBox(height: 24),
              Text('온보딩 준비 중', style: text.headlineSmall),
              const SizedBox(height: 12),
              Text(
                '키, 몸무게, 목표, 운동 요일을 입력하는 화면을 만들고 있어요.\n지금은 건너뛰고 둘러볼 수 있어요.',
                style: text.bodyLarge?.copyWith(color: AppColors.textSecondary),
              ),
              const Spacer(flex: 2),
              PrimaryButton(
                label: '건너뛰기',
                onPressed: () =>
                    ref.read(authControllerProvider.notifier).skipOnboarding(),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
