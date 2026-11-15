import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/theme/app_theme.dart';
import 'package:trainer_ssaem/router.dart';

class TrainerSsaemApp extends ConsumerWidget {
  const TrainerSsaemApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return MaterialApp.router(
      title: '트레이너쌤',
      debugShowCheckedModeBanner: false,
      // 다크만 둔다 (기기 설정이 라이트여도 다크)
      theme: AppTheme.dark,
      darkTheme: AppTheme.dark,
      themeMode: ThemeMode.dark,
      routerConfig: ref.watch(routerProvider),
    );
  }
}
