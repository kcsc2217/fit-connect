import 'package:flutter_test/flutter_test.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';

import '../../support/fake_http_adapter.dart';
import '../../support/fake_social_login_client.dart';
import '../../support/fixtures.dart';
import '../../support/test_app.dart';

/// 카카오가 id_token을 주고, 서버 로그인이 [response]로 답하는 상황
TestDeps _kakaoLoginAnswers(FakeResponse response) {
  final deps = TestDeps(
    clients: {
      AuthProvider.kakao: FakeSocialLoginClient(
        AuthProvider.kakao,
        idToken: 'id-token',
      ),
    },
  );
  deps.adapter.on('POST', loginPath, (_) => response);
  return deps;
}

void main() {
  testWidgets('로그인 화면에 카카오와 구글 버튼이 보인다', (tester) async {
    // given
    final deps = TestDeps();

    // when
    await pumpTestApp(tester, deps);

    // then
    expect(find.text('카카오로 시작하기'), findsOneWidget);
    expect(find.text('Google로 시작하기'), findsOneWidget);
  });

  testWidgets('개발용 로그인이 꺼져 있으면 개발용 버튼이 보이지 않는다', (tester) async {
    // given
    final deps = TestDeps(devLoginEnabled: false);

    // when
    await pumpTestApp(tester, deps);

    // then
    expect(find.text('개발용 로그인'), findsNothing);
  });

  testWidgets('SOCIAL_LOGIN_UNAVAILABLE이면 잠시 후 다시 시도하라는 문구를 보여준다', (
    tester,
  ) async {
    // given
    final deps = _kakaoLoginAnswers(
      FakeResponse.problem(503, 'SOCIAL_LOGIN_UNAVAILABLE'),
    );
    await pumpTestApp(tester, deps);

    // when
    await tester.tap(find.text('카카오로 시작하기'));
    await tester.pumpAndSettle();

    // then
    expect(find.textContaining('잠시 후 다시 시도해 주세요'), findsOneWidget);
  });

  testWidgets('INVALID_SOCIAL_TOKEN이면 로그인하지 못했다는 문구를 보여준다', (tester) async {
    // given
    final deps = _kakaoLoginAnswers(
      FakeResponse.problem(401, 'INVALID_SOCIAL_TOKEN'),
    );
    await pumpTestApp(tester, deps);

    // when
    await tester.tap(find.text('카카오로 시작하기'));
    await tester.pumpAndSettle();

    // then
    expect(find.textContaining('로그인하지 못했어요'), findsOneWidget);
  });
}
