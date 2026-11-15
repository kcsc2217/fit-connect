import 'package:material_ui/material_ui.dart';

/// 색 토큰. 기준: docs/design/reference-dark-lime.webp (다크 배경 + 라임 강조색 하나)
abstract final class AppColors {
  /// 화면 배경 (거의 검정, 살짝 초록 기운)
  static const background = Color(0xFF0B0C0A);

  /// 카드 배경 (배경보다 한 단계 밝게)
  static const surface = Color(0xFF17191A);

  /// 카드 안의 칸, 눌린 상태
  static const surfaceHigh = Color(0xFF222521);

  /// 카드 테두리
  static const border = Color(0xFF2C2F2A);

  /// 강조색. 주요 버튼, 선택된 탭, 진행 표시에만 쓴다
  static const lime = Color(0xFFC6F432);

  /// 라임 위의 글자
  static const onLime = Color(0xFF0B0C0A);

  static const textPrimary = Color(0xFFFFFFFF);
  static const textSecondary = Color(0xFF9A9E95);
  static const textTertiary = Color(0xFF6B6F67);

  static const error = Color(0xFFFF7A6B);

  /// 소셜 로그인 버튼 (각 회사 디자인 가이드 색)
  static const kakaoYellow = Color(0xFFFEE500);
  static const kakaoLabel = Color(0xD9000000);
  static const googleWhite = Color(0xFFFFFFFF);
  static const googleLabel = Color(0xFF1F1F1F);
}
