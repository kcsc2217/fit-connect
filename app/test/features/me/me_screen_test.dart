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
        createdAt: '2026-11-05T03:00:00Z',
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
    expect(
      (find.text('회원1234'), find.text('2026.11.05'), find.text('카카오')),
      (findsOneWidget, findsOneWidget, findsOneWidget),
    );
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
}
