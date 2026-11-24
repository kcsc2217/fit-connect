import 'package:flutter_test/flutter_test.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';

import 'support/fake_http_adapter.dart';
import 'support/fake_social_login_client.dart';
import 'support/fixtures.dart';
import 'support/memory_token_storage.dart';
import 'support/test_app.dart';

/// 카카오 로그인 결과 회원 상태가 [status]인 상황
TestDeps _kakaoLoginAs(String status) {
  final deps = TestDeps(
    clients: {
      AuthProvider.kakao: FakeSocialLoginClient(
        AuthProvider.kakao,
        idToken: 'id-token',
      ),
    },
  );
  deps.adapter.on(
    'POST',
    loginPath,
    (_) => FakeResponse(200, loginJson('a', 'r', status: status)),
  );
  return deps;
}

void main() {
  testWidgets('ONBOARDING 회원이 로그인하면 온보딩 1단계 화면으로 간다', (tester) async {
    // given
    await pumpTestApp(tester, _kakaoLoginAs('ONBOARDING'));

    // when
    await tester.tap(find.text('카카오로 시작하기'));
    await tester.pumpAndSettle();

    // then
    expect(find.text('몸 정보를 알려주세요'), findsOneWidget);
  });

  testWidgets('ACTIVE 회원이 로그인하면 오늘 탭으로 간다', (tester) async {
    // given
    await pumpTestApp(tester, _kakaoLoginAs('ACTIVE'));

    // when
    await tester.tap(find.text('카카오로 시작하기'));
    await tester.pumpAndSettle();

    // then
    expect(find.text('오늘의 운동'), findsOneWidget);
  });

  testWidgets('세션이 만료되면 어느 탭에 있든 로그인 화면으로 간다', (tester) async {
    // given: 앱 시작 때 내 정보는 성공, "나" 탭에서 다시 부를 때는 UNAUTHORIZED
    final deps = TestDeps(storage: MemoryTokenStorage(tokens('a', 'r')));
    var meCalls = 0;
    deps.adapter.on(
      'GET',
      mePath,
      (_) => ++meCalls == 1
          ? FakeResponse(200, meJson())
          : FakeResponse.problem(401, 'UNAUTHORIZED'),
    );
    await pumpTestApp(tester, deps);

    // when
    await tester.tap(find.text('나'));
    await tester.pumpAndSettle();

    // then
    expect(find.text('카카오로 시작하기'), findsOneWidget);
  });

  testWidgets('앱 시작 중 서버 오류면 연결 문제가 아니라 문제가 생겼다는 문구를 보여준다', (tester) async {
    // given
    final deps = TestDeps(storage: MemoryTokenStorage(tokens('a', 'r')));
    deps.adapter.on(
      'GET',
      mePath,
      (_) => FakeResponse.problem(500, 'INTERNAL_ERROR'),
    );

    // when
    await pumpTestApp(tester, deps);

    // then
    expect(find.text('문제가 생겼어요'), findsOneWidget);
  });
}
