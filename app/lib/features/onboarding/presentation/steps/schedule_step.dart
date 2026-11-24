import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/widgets/choice_chips.dart';
import 'package:trainer_ssaem/features/onboarding/domain/onboarding_draft.dart';
import 'package:trainer_ssaem/features/onboarding/domain/profile_models.dart';
import 'package:trainer_ssaem/features/onboarding/presentation/labels.dart';
import 'package:trainer_ssaem/features/onboarding/presentation/steps/alarm_time_picker.dart';
import 'package:trainer_ssaem/features/onboarding/presentation/steps/step_header.dart';

/// 3단계: 운동 요일, 분할, 알림 시각, 운동 시간
class ScheduleStep extends StatelessWidget {
  const ScheduleStep({required this.draft, required this.onEdit, super.key});

  final OnboardingDraft draft;
  final void Function(OnboardingDraft Function(OnboardingDraft)) onEdit;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const StepHeader(
          title: '언제 운동하나요?',
          description: '운동하는 날, 설정한 시각에 오늘의 운동을 보내드려요.',
        ),
        FormSection(
          label: '운동 요일',
          child: ChoiceChips(
            options: dayOptions,
            selected: draft.days,
            onTap: (day) => onEdit((d) {
              final next = {...d.days};
              if (!next.remove(day)) next.add(day);
              return d.copyWith(days: next);
            }),
          ),
        ),
        FormSection(
          label: '분할 방식',
          child: ChoiceChips(
            options: splitOptions,
            selected: {?draft.split},
            onTap: (v) => onEdit((d) => d.copyWith(split: v)),
          ),
        ),
        FormSection(
          label: '알림 받을 시각',
          child: AlarmTimePicker(
            value: draft.alarm,
            onChanged: (t) => onEdit((d) => d.copyWith(alarm: t)),
          ),
        ),
        FormSection(
          label: '운동 시간',
          child: ChoiceChips(
            options: [for (final m in sessionMinuteOptions) (m, '$m분')],
            selected: {draft.sessionMinutes},
            onTap: (v) => onEdit((d) => d.copyWith(sessionMinutes: v)),
          ),
        ),
      ],
    );
  }
}
