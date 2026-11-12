import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_riverpod/misc.dart' show Override;
import 'package:flutter_test/flutter_test.dart';
import 'package:trainer_ssaem/app.dart';
import 'package:trainer_ssaem/core/config/app_config.dart';
import 'package:trainer_ssaem/core/network/http_client.dart';
import 'package:trainer_ssaem/features/auth/data/social/social_login_client.dart';
import 'package:trainer_ssaem/features/auth/data/token_storage.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';

import 'fake_http_adapter.dart';
import 'fake_social_login_client.dart';
import 'memory_token_storage.dart';

/// 테스트가 바꿔 끼우는 협력 객체 묶음
class TestDeps {
  TestDeps({
    FakeHttpAdapter? adapter,
    MemoryTokenStorage? storage,
    Map<AuthProvider, SocialLoginClient>? clients,
    this.devLoginEnabled = false,
  }) : adapter = adapter ?? FakeHttpAdapter(),
       storage = storage ?? MemoryTokenStorage(),
       clients =
           clients ??
           {
             AuthProvider.kakao: FakeSocialLoginClient(AuthProvider.kakao),
             AuthProvider.google: FakeSocialLoginClient(AuthProvider.google),
           };

  final FakeHttpAdapter adapter;
  final MemoryTokenStorage storage;
  final Map<AuthProvider, SocialLoginClient> clients;
  final bool devLoginEnabled;

  List<Override> get overrides => [
    appConfigProvider.overrideWithValue(
      AppConfig(apiBaseUrl: 'http://test', devLoginEnabled: devLoginEnabled),
    ),
    httpClientAdapterProvider.overrideWithValue(adapter),
    tokenStorageProvider.overrideWithValue(storage),
    socialLoginClientsProvider.overrideWithValue(clients),
  ];

  /// 단위 테스트용 컨테이너. 실패한 provider를 자동 재시도하지 않는다 (Riverpod 3 기본값 끔).
  ProviderContainer container() =>
      ProviderContainer.test(overrides: overrides, retry: (_, _) => null);
}

/// 앱 전체를 띄우고 화면이 멈출 때까지 기다린다.
Future<void> pumpTestApp(WidgetTester tester, TestDeps deps) async {
  await tester.pumpWidget(
    ProviderScope(
      overrides: deps.overrides,
      retry: (_, _) => null,
      child: const TrainerSsaemApp(),
    ),
  );
  await tester.pumpAndSettle();
}
