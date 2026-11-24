import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/widgets/choice_chips.dart';
import 'package:trainer_ssaem/core/widgets/unit_text_field.dart';
import 'package:trainer_ssaem/features/onboarding/domain/onboarding_draft.dart';
import 'package:trainer_ssaem/features/onboarding/presentation/labels.dart';
import 'package:trainer_ssaem/features/onboarding/presentation/steps/step_header.dart';

/// 1단계: 키, 몸무게, 나이, 성별
class BodyStep extends StatelessWidget {
  const BodyStep({required this.draft, required this.onEdit, super.key});

  final OnboardingDraft draft;
  final void Function(OnboardingDraft Function(OnboardingDraft)) onEdit;

  /// 값을 넣었는데 범위 밖일 때만 문구를 보인다 (빈 칸은 아직 입력 전)
  String? _error(String text, bool valid, String message) =>
      text.isEmpty || valid ? null : message;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const StepHeader(
          title: '몸 정보를 알려주세요',
          description: '목표 칼로리와 시작 무게를 계산하는 데 써요.',
        ),
        FormSection(
          label: '키',
          child: UnitTextField(
            fieldKey: const ValueKey('height'),
            initialValue: draft.heightText,
            unit: 'cm',
            decimal: true,
            errorText: _error(
              draft.heightText,
              draft.heightValid,
              '100~250cm 사이로 입력해 주세요',
            ),
            onChanged: (v) => onEdit((d) => d.copyWith(heightText: v)),
          ),
        ),
        FormSection(
          label: '몸무게',
          child: UnitTextField(
            fieldKey: const ValueKey('weight'),
            initialValue: draft.weightText,
            unit: 'kg',
            decimal: true,
            errorText: _error(
              draft.weightText,
              draft.weightValid,
              '30~300kg, 소수 한 자리까지 입력해 주세요',
            ),
            onChanged: (v) => onEdit((d) => d.copyWith(weightText: v)),
          ),
        ),
        FormSection(
          label: '나이',
          child: UnitTextField(
            fieldKey: const ValueKey('age'),
            initialValue: draft.ageText,
            unit: '세',
            errorText: _error(
              draft.ageText,
              draft.ageValid,
              '14~100세 사이로 입력해 주세요',
            ),
            onChanged: (v) => onEdit((d) => d.copyWith(ageText: v)),
          ),
        ),
        FormSection(
          label: '성별',
          child: ChoiceChips(
            options: sexOptions,
            selected: {?draft.sex},
            onTap: (v) => onEdit((d) => d.copyWith(sex: v)),
          ),
        ),
      ],
    );
  }
}
