import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/theme/app_colors.dart';

/// 라임 원 안에 아령 아이콘. 레퍼런스의 빛나는 원형 강조를 단순하게 옮겼다.
class AppLogo extends StatelessWidget {
  const AppLogo({this.size = 64, super.key});

  final double size;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: AppColors.lime,
        shape: BoxShape.circle,
        boxShadow: [
          BoxShadow(
            color: AppColors.lime.withValues(alpha: 0.35),
            blurRadius: size * 0.5,
          ),
        ],
      ),
      child: Icon(
        Icons.fitness_center_rounded,
        color: AppColors.onLime,
        size: size * 0.5,
      ),
    );
  }
}
