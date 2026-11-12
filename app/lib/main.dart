import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/app.dart';
import 'package:trainer_ssaem/core/config/app_config.dart';

/// 실행: flutter run --dart-define-from-file=config/local.json
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final config = AppConfig.fromEnvironment();
  if (config.kakaoNativeAppKey.isNotEmpty) {
    await KakaoSdk.init(nativeAppKey: config.kakaoNativeAppKey);
  }

  runApp(
    ProviderScope(
      overrides: [appConfigProvider.overrideWithValue(config)],
      child: const TrainerSsaemApp(),
    ),
  );
}
