import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/theme/app_colors.dart';

/// 모서리 반지름 토큰
abstract final class AppRadius {
  static const card = 24.0;
  static const tile = 16.0;
  static const pill = 999.0;
}

/// 다크 테마 하나만 둔다 (명세 열린 질문 7). 글꼴은 Pretendard.
abstract final class AppTheme {
  static const fontFamily = 'Pretendard';

  static ThemeData get dark {
    const scheme = ColorScheme.dark(
      primary: AppColors.lime,
      onPrimary: AppColors.onLime,
      secondary: AppColors.lime,
      onSecondary: AppColors.onLime,
      surface: AppColors.background,
      onSurface: AppColors.textPrimary,
      onSurfaceVariant: AppColors.textSecondary,
      surfaceContainer: AppColors.surface,
      surfaceContainerHigh: AppColors.surfaceHigh,
      outline: AppColors.border,
      outlineVariant: AppColors.border,
      error: AppColors.error,
    );

    final base = ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      colorScheme: scheme,
      fontFamily: fontFamily,
      scaffoldBackgroundColor: AppColors.background,
    );

    return base.copyWith(
      textTheme: base.textTheme
          .copyWith(
            displaySmall: const TextStyle(
              fontSize: 34,
              fontWeight: FontWeight.w800,
              height: 1.15,
              letterSpacing: -0.5,
            ),
            headlineSmall: const TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.w800,
              letterSpacing: -0.3,
            ),
            titleMedium: const TextStyle(
              fontSize: 17,
              fontWeight: FontWeight.w700,
            ),
            bodyLarge: const TextStyle(fontSize: 16, height: 1.5),
            bodyMedium: const TextStyle(fontSize: 14, height: 1.45),
            labelLarge: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w700,
            ),
          )
          .apply(
            bodyColor: AppColors.textPrimary,
            displayColor: AppColors.textPrimary,
          ),
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.background,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: true,
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: AppColors.surface,
        indicatorColor: AppColors.lime.withValues(alpha: 0.16),
        height: 68,
        labelTextStyle: WidgetStateProperty.resolveWith(
          (states) => TextStyle(
            fontFamily: fontFamily,
            fontSize: 12,
            fontWeight: FontWeight.w600,
            color: states.contains(WidgetState.selected)
                ? AppColors.lime
                : AppColors.textTertiary,
          ),
        ),
        iconTheme: WidgetStateProperty.resolveWith(
          (states) => IconThemeData(
            size: 24,
            color: states.contains(WidgetState.selected)
                ? AppColors.lime
                : AppColors.textTertiary,
          ),
        ),
      ),
      snackBarTheme: const SnackBarThemeData(
        backgroundColor: AppColors.surfaceHigh,
        contentTextStyle: TextStyle(
          fontFamily: fontFamily,
          color: AppColors.textPrimary,
        ),
        behavior: SnackBarBehavior.floating,
      ),
      dividerTheme: const DividerThemeData(
        color: AppColors.border,
        thickness: 1,
        space: 1,
      ),
      progressIndicatorTheme: const ProgressIndicatorThemeData(
        color: AppColors.lime,
      ),
    );
  }
}
