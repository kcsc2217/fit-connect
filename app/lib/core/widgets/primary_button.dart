import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/theme/app_colors.dart';
import 'package:trainer_ssaem/core/theme/app_theme.dart';

/// 알약 모양의 큰 버튼. 기본은 라임(주요 동작), 색을 바꿔 소셜 로그인 버튼에도 쓴다.
class PrimaryButton extends StatelessWidget {
  const PrimaryButton({
    required this.label,
    required this.onPressed,
    this.leading,
    this.background = AppColors.lime,
    this.foreground = AppColors.onLime,
    this.loading = false,
    super.key,
  });

  /// 테두리만 있는 어두운 버튼 (보조 동작)
  const PrimaryButton.outlined({
    required this.label,
    required this.onPressed,
    this.leading,
    this.loading = false,
    super.key,
  }) : background = AppColors.surface,
       foreground = AppColors.textPrimary;

  final String label;

  /// null이면 비활성
  final VoidCallback? onPressed;
  final Widget? leading;
  final Color background;
  final Color foreground;

  /// 진행 중이면 글자 대신 작은 원형 진행 표시
  final bool loading;

  @override
  Widget build(BuildContext context) {
    final outlined = background == AppColors.surface;
    return SizedBox(
      height: 56,
      width: double.infinity,
      child: FilledButton(
        onPressed: loading ? null : onPressed,
        style: FilledButton.styleFrom(
          backgroundColor: background,
          foregroundColor: foreground,
          disabledBackgroundColor: background.withValues(alpha: 0.5),
          disabledForegroundColor: foreground.withValues(alpha: 0.6),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppRadius.pill),
            side: outlined
                ? const BorderSide(color: AppColors.border)
                : BorderSide.none,
          ),
          textStyle: Theme.of(context).textTheme.labelLarge,
        ),
        child: loading
            ? SizedBox.square(
                dimension: 20,
                child: CircularProgressIndicator(
                  strokeWidth: 2.4,
                  color: foreground,
                ),
              )
            : Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  if (leading != null) ...[leading!, const SizedBox(width: 10)],
                  Text(label),
                ],
              ),
      ),
    );
  }
}
