import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/theme/app_colors.dart';
import 'package:trainer_ssaem/core/theme/app_theme.dart';

/// 배경보다 한 단계 밝은 둥근 카드 + 얇은 테두리
class AppCard extends StatelessWidget {
  const AppCard({
    required this.child,
    this.padding = const EdgeInsets.all(20),
    super.key,
  });

  final Widget child;
  final EdgeInsetsGeometry padding;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppRadius.card),
        border: Border.all(color: AppColors.border),
      ),
      child: Padding(padding: padding, child: child),
    );
  }
}
