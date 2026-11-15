import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_riverpod/misc.dart' show Override;
import 'package:flutter_test/flutter_test.dart';
import 'package:trainer_ssaem/app.dart';
import 'package:trainer_ssaem/core/config/app_config.dart';
import 'package:trainer_ssaem/core/network/http_client.dart';
import 'package:trainer_ssaem/core/network/retry_policy.dart';
import 'package:trainer_ssaem/features/auth/data/social/social_login_client.dart';
import 'package:trainer_ssaem/features/auth/data/token_storage.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';

import 'fake_http_adapter.dart';
import 'memory_token_storage.dart';

/// 테스트가 바꿔 끼우는 협력 객체 묶음
class TestDeps {
  TestDeps({
    FakeHttpAdapter? adapter,
    MemoryTokenStorage? storage,
    this.clients,
    this.devLoginEnabled = false,
  }) : adapter = adapter ?? FakeHttpAdapter(),
       storage = storage ?? MemoryTokenStorage();

  final FakeHttpAdapter adapter;
  final MemoryTokenStorage storage;

  /// 소셜 SDK 가짜. null이면 실제 socialLoginClientsProvider를 쓴다
  /// (설정에 따라 어떤 버튼이 보이는지 검증할 때. 키가 없어 SDK는 부르지 않는다)
  final Map<AuthProvider, SocialLoginClient>? clients;
  final bool devLoginEnabled;

  List<Override> get overrides => [
    appConfigProvider.overrideWithValue(
      AppConfig(apiBaseUrl: 'http://test', devLoginEnabled: devLoginEnabled),
    ),
    httpClientAdapterProvider.overrideWithValue(adapter),
    tokenStorageProvider.overrideWithValue(storage),
    if (clients != null) socialLoginClientsProvider.overrideWithValue(clients!),
  ];

  /// 단위 테스트용 컨테이너. 재시도 정책은 앱과 같다 (appRetry).
  ProviderContainer container() =>
      ProviderContainer.test(overrides: overrides, retry: appRetry);
}

/// 앱 전체를 띄우고 화면이 멈출 때까지 기다린다.
Future<void> pumpTestApp(WidgetTester tester, TestDeps deps) async {
  await tester.pumpWidget(
    ProviderScope(
      overrides: deps.overrides,
      retry: appRetry,
      child: const TrainerSsaemApp(),
    ),
  );
  await tester.pumpAndSettle();
}
