import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/theme/app_colors.dart';
import 'package:trainer_ssaem/core/widgets/primary_button.dart';
import 'package:trainer_ssaem/features/onboarding/domain/onboarding_controller.dart';
import 'package:trainer_ssaem/features/onboarding/presentation/steps/body_step.dart';
import 'package:trainer_ssaem/features/onboarding/presentation/steps/goal_step.dart';
import 'package:trainer_ssaem/features/onboarding/presentation/steps/schedule_step.dart';

/// 온보딩 3단계 입력 화면 (프로토타입 onboard(), 계획 010).
/// 제출이 성공하면 로그인 상태가 Active가 되고, 라우터가 오늘 탭으로 보낸다.
class OnboardingScreen extends ConsumerWidget {
  const OnboardingScreen({super.key});

  static const _stepCount = OnboardingState.lastStep + 1;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(onboardingControllerProvider);
    final controller = ref.read(onboardingControllerProvider.notifier);
    final step = state.step;
    final isLast = step == OnboardingState.lastStep;
    final text = Theme.of(context).textTheme;

    final body = switch (step) {
      0 => BodyStep(draft: state.draft, onEdit: controller.edit),
      1 => GoalStep(draft: state.draft, onEdit: controller.edit),
      _ => ScheduleStep(draft: state.draft, onEdit: controller.edit),
    };

    // 숫자 키패드에는 닫기 키가 없어, 입력칸 밖을 누르면 키보드를 닫는다
    return GestureDetector(
      behavior: HitTestBehavior.translucent,
      onTap: () => FocusScope.of(context).unfocus(),
      child: Scaffold(
        body: SafeArea(
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(8, 4, 24, 0),
                child: Row(
                  children: [
                    if (step > 0)
                      IconButton(
                        tooltip: '이전',
                        icon: const Icon(Icons.arrow_back_ios_new_rounded),
                        onPressed: state.submitting ? null : controller.back,
                      )
                    else
                      const SizedBox(width: 48, height: 48),
                    const Spacer(),
                    Text(
                      '${step + 1} / $_stepCount',
                      style: text.bodyMedium?.copyWith(
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(24, 8, 24, 24),
                child: Row(
                  children: [
                    for (var i = 0; i < _stepCount; i++) ...[
                      if (i > 0) const SizedBox(width: 6),
                      Expanded(
                        child: Container(
                          height: 4,
                          decoration: BoxDecoration(
                            color: i <= step
                                ? AppColors.textPrimary
                                : AppColors.border,
                            borderRadius: BorderRadius.circular(2),
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              Expanded(
                child: SingleChildScrollView(
                  // 단계마다 입력칸을 새로 만들어 "이전"으로 돌아왔을 때 입력값으로 다시 채운다
                  key: ValueKey(step),
                  padding: const EdgeInsets.symmetric(horizontal: 24),
                  keyboardDismissBehavior:
                      ScrollViewKeyboardDismissBehavior.onDrag,
                  // 입력칸이 없는 단계(칩만 있는 2·3단계)도 왼쪽 여백에서 시작하도록 폭을 채운다
                  child: SizedBox(
                    width: double.infinity,
                    // 보내는 중에는 선택지를 누를 수 없게 한다
                    child: IgnorePointer(
                      ignoring: state.submitting,
                      child: body,
                    ),
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(24, 8, 24, 20),
                child: Column(
                  children: [
                    if (state.failure != null) ...[
                      Text(
                        switch (state.failure!) {
                          SubmitFailure.network => '연결을 확인해 주세요',
                          SubmitFailure.invalidInput => '입력한 정보를 다시 확인해 주세요',
                          SubmitFailure.server => '문제가 생겼어요. 잠시 후 다시 시도해 주세요',
                        },
                        style: text.bodyMedium?.copyWith(
                          color: AppColors.error,
                        ),
                      ),
                      const SizedBox(height: 12),
                    ],
                    PrimaryButton(
                      label: isLast ? '오늘의 운동 받기' : '다음',
                      loading: state.submitting,
                      onPressed: controller.currentStepValid
                          ? (isLast ? controller.submit : controller.next)
                          : null,
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
