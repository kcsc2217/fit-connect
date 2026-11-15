import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_controller.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_state.dart';

import '../../support/fake_http_adapter.dart';
import '../../support/fake_social_login_client.dart';
import '../../support/fixtures.dart';
import '../../support/memory_token_storage.dart';
import '../../support/test_app.dart';

void main() {
  test('저장된 refresh가 없으면 시작 상태는 로그아웃이다', () async {
    // given
    final container = TestDeps().container();

    // when
    final state = await container.read(authControllerProvider.future);

    // then
    expect(state, isA<SignedOut>());
  });

  test('저장된 토큰으로 내 정보가 ACTIVE면 시작 상태는 active다', () async {
    // given
    final deps = TestDeps(storage: MemoryTokenStorage(tokens('a', 'r')));
    deps.adapter.on('GET', mePath, (_) => FakeResponse(200, meJson()));
    final container = deps.container();

    // when
    final state = await container.read(authControllerProvider.future);

    // then
    expect(state, isA<Active>());
  });

  test('저장된 토큰으로 내 정보가 ONBOARDING이면 시작 상태는 onboarding이다', () async {
    // given
    final deps = TestDeps(storage: MemoryTokenStorage(tokens('a', 'r')));
    deps.adapter.on(
      'GET',
      mePath,
      (_) => FakeResponse(200, meJson(status: 'ONBOARDING')),
    );
    final container = deps.container();

    // when
    final state = await container.read(authControllerProvider.future);

    // then
    expect(state, isA<Onboarding>());
  });

  test('앱 시작 중 네트워크가 끊기면 토큰을 유지하고 시작 오류 상태가 된다', () async {
    // given
    final deps = TestDeps(storage: MemoryTokenStorage(tokens('a', 'r')));
    deps.adapter.on('GET', mePath, (req) => throw offline(req));
    final container = deps.container();

    // when
    final state = await container.read(authControllerProvider.future);

    // then
    expect(
      (state is StartupError, deps.storage.tokens?.refreshToken),
      (true, 'r'),
    );
  });

  test('저장된 토큰의 회원이 서버에 없으면 토큰을 지우고 로그아웃 상태가 된다', () async {
    // given: 토큰은 유효하지만 회원이 지워져 내 정보가 404
    final deps = TestDeps(storage: MemoryTokenStorage(tokens('a', 'r')));
    deps.adapter.on(
      'GET',
      mePath,
      (_) => FakeResponse.problem(404, 'NOT_FOUND'),
    );
    final container = deps.container();

    // when
    final state = await container.read(authControllerProvider.future);

    // then
    expect((state is SignedOut, deps.storage.tokens), (true, null));
  });

  test('로그인에 성공하면 토큰 4개를 저장한다', () async {
    // given
    final kakao = FakeSocialLoginClient(
      AuthProvider.kakao,
      idToken: 'id-token',
    );
    final deps = TestDeps(clients: {AuthProvider.kakao: kakao});
    deps.adapter.on(
      'POST',
      loginPath,
      (_) => FakeResponse(200, loginJson('access-1', 'refresh-1')),
    );
    final container = deps.container();
    await container.read(authControllerProvider.future);

    // when
    await container
        .read(authControllerProvider.notifier)
        .signIn(AuthProvider.kakao);

    // then
    expect(deps.storage.tokens, tokens('access-1', 'refresh-1'));
  });

  test('소셜 로그인 창을 닫으면 서버를 호출하지 않고 로그아웃 상태를 유지한다', () async {
    // given
    final kakao = FakeSocialLoginClient(AuthProvider.kakao); // signIn → null
    final deps = TestDeps(clients: {AuthProvider.kakao: kakao});
    final container = deps.container();
    await container.read(authControllerProvider.future);

    // when
    await container
        .read(authControllerProvider.notifier)
        .signIn(AuthProvider.kakao);

    // then
    expect(
      (
        deps.adapter.requests.length,
        container.read(authControllerProvider).value is SignedOut,
      ),
      (0, true),
    );
  });

  test('로그아웃 요청이 실패해도 기기 토큰을 지우고 소셜 SDK에서도 로그아웃한다', () async {
    // given
    final kakao = FakeSocialLoginClient(AuthProvider.kakao);
    final deps = TestDeps(
      storage: MemoryTokenStorage(tokens('a', 'r')),
      clients: {AuthProvider.kakao: kakao},
    );
    deps.adapter.on('GET', mePath, (_) => FakeResponse(200, meJson()));
    deps.adapter.on('POST', logoutPath, (req) => throw offline(req));
    final container = deps.container();
    await container.read(authControllerProvider.future);

    // when
    await container.read(authControllerProvider.notifier).signOut();

    // then
    expect(deps.storage.tokens, isNull);
    verify(kakao.signOut).called(1);
  });
}
