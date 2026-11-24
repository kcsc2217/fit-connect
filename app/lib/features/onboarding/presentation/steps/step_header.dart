import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/theme/app_colors.dart';

/// 단계 제목과 설명 (프로토타입 `.q`, `.muted.small`)
class StepHeader extends StatelessWidget {
  const StepHeader({required this.title, required this.description, super.key});

  final String title;
  final String description;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.only(bottom: 28),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: text.headlineSmall),
          const SizedBox(height: 8),
          Text(
            description,
            style: text.bodyMedium?.copyWith(color: AppColors.textSecondary),
          ),
        ],
      ),
    );
  }
}
