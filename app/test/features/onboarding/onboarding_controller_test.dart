import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_controller.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_state.dart';
import 'package:trainer_ssaem/features/onboarding/domain/onboarding_controller.dart';

import '../../support/fake_http_adapter.dart';
import '../../support/fixtures.dart';
import '../../support/memory_token_storage.dart';
import '../../support/onboarding_steps.dart';
import '../../support/test_app.dart';

/// 로그인된 ONBOARDING 회원으로 시작 판정을 끝내고, 세 단계를 채운 컨트롤러를 준비한다
Future<(ProviderContainer, OnboardingController)> _ready(TestDeps deps) async {
  final container = deps.container();
  await container.read(authControllerProvider.future);
  // autoDispose라 읽는 동안 살아 있게 붙잡는다
  container.listen(onboardingControllerProvider, (_, _) {});
  final controller = container.read(onboardingControllerProvider.notifier)
    ..edit((_) => completeDraft);
  return (container, controller);
}

void main() {
  test('제출에 성공하면 로그인 상태가 active가 된다', () async {
    // given
    final (container, controller) = await _ready(onboardingUser());

    // when
    await controller.submit();

    // then
    expect(container.read(authControllerProvider).value, isA<Active>());
  });

  test('제출 중 연결이 끊기면 입력값을 유지하고 연결 실패를 알린다', () async {
    // given
    final deps = onboardingUser();
    deps.adapter.on('PUT', profilePath, (req) => throw offline(req));
    final (container, controller) = await _ready(deps);

    // when
    await controller.submit();

    // then
    final state = container.read(onboardingControllerProvider);
    expect(
      (state.failure, state.draft.heightText, state.submitting),
      (SubmitFailure.network, '175', false),
    );
  });

  test('제출이 서버 오류면 문제가 생겼다는 실패를 알린다', () async {
    // given
    final deps = onboardingUser();
    deps.adapter.on(
      'PUT',
      profilePath,
      (_) => FakeResponse.problem(500, 'INTERNAL_ERROR'),
    );
    final (container, controller) = await _ready(deps);

    // when
    await controller.submit();

    // then
    expect(
      container.read(onboardingControllerProvider).failure,
      SubmitFailure.server,
    );
  });

  test('제출이 입력값 오류면 입력을 확인하라는 실패를 알린다', () async {
    // given: 연말 경계 등으로 서버가 출생 연도를 거절한 경우
    final deps = onboardingUser();
    deps.adapter.on(
      'PUT',
      profilePath,
      (_) => FakeResponse.problem(400, 'INVALID_INPUT'),
    );
    final (container, controller) = await _ready(deps);

    // when
    await controller.submit();

    // then
    expect(
      container.read(onboardingControllerProvider).failure,
      SubmitFailure.invalidInput,
    );
  });

  test('보내는 중에 다시 제출해도 요청은 한 번만 보낸다', () async {
    // given: 서버 응답을 붙잡아 두어 첫 제출이 끝나지 않게 한다
    final deps = onboardingUser();
    final release = Completer<void>();
    deps.adapter.on('PUT', profilePath, (_) async {
      await release.future;
      return const FakeResponse(200, {'status': 'ACTIVE'});
    });
    final (_, controller) = await _ready(deps);
    final first = controller.submit();

    // when
    await controller.submit();
    release.complete();
    await first;

    // then
    expect(deps.adapter.count('PUT', profilePath), 1);
  });

  test('제출 결과가 회원 없음이면 로그아웃 상태가 된다', () async {
    // given: 토큰은 유효한데 서버에 회원이 없다
    final deps = onboardingUser();
    deps.adapter.on(
      'PUT',
      profilePath,
      (_) => FakeResponse.problem(404, 'NOT_FOUND'),
    );
    final (container, controller) = await _ready(deps);

    // when
    await controller.submit();

    // then
    expect(container.read(authControllerProvider).value, isA<SignedOut>());
  });

  test('기기 시간대를 읽지 못하면 Asia/Seoul로 보낸다', () async {
    // given: 플러그인이 실패하는 기기
    final deps = TestDeps(
      storage: MemoryTokenStorage(tokens('a', 'r')),
      timeZoneReader: () async => throw Exception('플러그인 없음'),
    );
    deps.adapter
      ..on('GET', mePath, (_) => FakeResponse(200, meJson(status: 'ONBOARDING')))
      ..on('PUT', profilePath, (_) => const FakeResponse(200, {'status': 'ACTIVE'}));
    final (_, controller) = await _ready(deps);

    // when
    await controller.submit();

    // then
    final sent =
        deps.adapter.requests.lastWhere((r) => r.path == profilePath).data
            as Map<String, Object?>;
    expect(sent['timezone'], 'Asia/Seoul');
  });
}
