import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/widgets/choice_chips.dart';
import 'package:trainer_ssaem/features/onboarding/domain/onboarding_draft.dart';
import 'package:trainer_ssaem/features/onboarding/domain/profile_models.dart';
import 'package:trainer_ssaem/features/onboarding/presentation/labels.dart';
import 'package:trainer_ssaem/features/onboarding/presentation/steps/step_header.dart';

/// 2단계: 목표, 경력, 장비
class GoalStep extends StatelessWidget {
  const GoalStep({required this.draft, required this.onEdit, super.key});

  final OnboardingDraft draft;
  final void Function(OnboardingDraft Function(OnboardingDraft)) onEdit;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const StepHeader(
          title: '어떤 몸을 만들고 싶나요?',
          description: '목표에 따라 세트와 반복 수가 달라져요.',
        ),
        FormSection(
          label: '목표',
          child: ChoiceChips(
            options: goalOptions,
            selected: {?draft.goal},
            onTap: (v) => onEdit((d) => d.copyWith(goal: v)),
          ),
        ),
        FormSection(
          label: '운동 경력',
          child: ChoiceChips(
            options: experienceOptions,
            selected: {?draft.experience},
            onTap: (v) => onEdit((d) => d.copyWith(experience: v)),
          ),
        ),
        FormSection(
          label: '쓸 수 있는 장비',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              ChoiceChips(
                options: equipmentChoiceOptions,
                selected: {?draft.equipmentChoice},
                onTap: (v) => onEdit((d) => d.copyWith(equipmentChoice: v)),
              ),
              if (draft.equipmentChoice == EquipmentChoice.custom) ...[
                const SizedBox(height: 16),
                ChoiceChips(
                  options: equipmentOptions,
                  selected: draft.customEquipment,
                  onTap: (v) => onEdit((d) {
                    final next = {...d.customEquipment};
                    if (!next.remove(v)) next.add(v);
                    return d.copyWith(customEquipment: next);
                  }),
                ),
              ],
            ],
          ),
        ),
      ],
    );
  }
}
