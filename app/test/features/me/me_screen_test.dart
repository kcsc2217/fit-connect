import 'package:flutter_test/flutter_test.dart';

import '../../support/fake_http_adapter.dart';
import '../../support/fixtures.dart';
import '../../support/memory_token_storage.dart';
import '../../support/test_app.dart';

/// 로그인된 ACTIVE 회원으로 앱을 띄운 상황
TestDeps _signedIn() {
  final deps = TestDeps(storage: MemoryTokenStorage(tokens('a', 'r')));
  deps.adapter.on(
    'GET',
    mePath,
    (_) => FakeResponse(
      200,
      meJson(
        nickname: '회원1234',
        createdAt: '2026-11-04T16:00:00Z', // UTC로는 11.04, 한국 시간으로는 11.05
        providers: ['KAKAO'],
      ),
    ),
  );
  deps.adapter.on('POST', logoutPath, (_) => const FakeResponse(204));
  return deps;
}

void main() {
  testWidgets('나 탭에 닉네임, 가입일, 연결된 소셜 종류를 보여준다', (tester) async {
    // given
    await pumpTestApp(tester, _signedIn());

    // when
    await tester.tap(find.text('나'));
    await tester.pumpAndSettle();

    // then
    expect(find.text('회원1234'), findsOneWidget);
    expect(find.text('2026.11.05'), findsOneWidget);
    expect(find.text('카카오'), findsOneWidget);
  });

  testWidgets('나 탭에서 로그아웃하면 로그인 화면으로 간다', (tester) async {
    // given
    await pumpTestApp(tester, _signedIn());
    await tester.tap(find.text('나'));
    await tester.pumpAndSettle();

    // when
    await tester.tap(find.text('로그아웃'));
    await tester.pumpAndSettle();

    // then
    expect(find.text('카카오로 시작하기'), findsOneWidget);
  });

  testWidgets('나 탭에서 연결이 끊기면 재시도를 기다리지 않고 다시 시도 버튼을 보여준다', (tester) async {
    // given: 앱 시작 때 내 정보는 성공, "나" 탭에서 다시 부를 때는 연결 실패
    final deps = TestDeps(storage: MemoryTokenStorage(tokens('a', 'r')));
    var meCalls = 0;
    deps.adapter.on('GET', mePath, (req) {
      if (++meCalls == 1) return FakeResponse(200, meJson());
      throw offline(req);
    });
    await pumpTestApp(tester, deps);

    // when
    await tester.tap(find.text('나'));
    await tester.pumpAndSettle();

    // then
    expect(find.text('다시 시도'), findsOneWidget);
  });
}
