import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/theme/app_colors.dart';

/// 알약 모양 선택 버튼 묶음 (프로토타입 `.choices`).
/// 하나 고르기·여러 개 고르기 모두 [selected]에 고른 값을 넣고, 누르면 [onTap]이 불린다.
class ChoiceChips<T> extends StatelessWidget {
  const ChoiceChips({
    required this.options,
    required this.selected,
    required this.onTap,
    super.key,
  });

  /// (값, 화면 이름)
  final List<(T, String)> options;
  final Set<T> selected;
  final ValueChanged<T> onTap;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        for (final (value, label) in options)
          _Chip(
            label: label,
            selected: selected.contains(value),
            onTap: () => onTap(value),
          ),
      ],
    );
  }
}

class _Chip extends StatelessWidget {
  const _Chip({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      selected: selected,
      child: Material(
        color: selected ? AppColors.lime : AppColors.surface,
        shape: StadiumBorder(
          side: BorderSide(color: selected ? AppColors.lime : AppColors.border),
        ),
        child: InkWell(
          customBorder: const StadiumBorder(),
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
            child: Text(
              label,
              style: Theme.of(context).textTheme.labelLarge?.copyWith(
                color: selected ? AppColors.onLime : AppColors.textPrimary,
                fontSize: 15,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// 입력 묶음 제목 + 내용 (프로토타입 `.field`)
class FormSection extends StatelessWidget {
  const FormSection({required this.label, required this.child, super.key});

  final String label;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 28),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: Theme.of(context).textTheme.bodyMedium
                ?.copyWith(color: AppColors.textSecondary),
          ),
          const SizedBox(height: 10),
          child,
        ],
      ),
    );
  }
}
