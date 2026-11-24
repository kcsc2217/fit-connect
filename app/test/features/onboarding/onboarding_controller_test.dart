import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_riverpod/misc.dart' show Override;
import 'package:flutter_test/flutter_test.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_controller.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_state.dart';
import 'package:trainer_ssaem/features/onboarding/data/device_time_zone.dart';
import 'package:trainer_ssaem/features/onboarding/data/profile_api.dart';
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

/// 로그인된 ONBOARDING 회원 (시간대 읽기·추가 override를 바꿔 끼울 수 있다)
TestDeps _onboardingDeps({
  Future<String> Function()? timeZoneReader,
  List<Override> extraOverrides = const [],
}) {
  final deps = TestDeps(
    storage: MemoryTokenStorage(tokens('a', 'r')),
    timeZoneReader: timeZoneReader,
    extraOverrides: extraOverrides,
  );
  deps.adapter
    ..on('GET', mePath, (_) => FakeResponse(200, meJson(status: 'ONBOARDING')))
    ..on(
      'PUT',
      profilePath,
      (_) => const FakeResponse(200, {'status': 'ACTIVE'}),
    );
  return deps;
}

/// 마지막으로 보낸 프로필의 시간대
Object? _sentTimezone(TestDeps deps) =>
    (deps.adapter.requests.lastWhere((r) => r.path == profilePath).data
        as Map<String, Object?>)['timezone'];

/// AppException이 아닌 예외를 던지는 API
class _ThrowingApi extends ProfileApi {
  _ThrowingApi() : super(Dio());

  @override
  Future<void> submit(Map<String, Object?> body) async =>
      throw StateError('예상하지 못한 오류');
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
    final second = controller.submit();

    // then: 가드가 없으면 두 번째도 요청을 보내 2가 된다 (응답을 풀어 준 뒤 센다)
    release.complete();
    await Future.wait([first, second]);
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
    final deps = _onboardingDeps(
      timeZoneReader: () async => throw Exception('플러그인 없음'),
    );
    final (_, controller) = await _ready(deps);

    // when
    await controller.submit();

    // then
    expect(_sentTimezone(deps), 'Asia/Seoul');
  });

  test('시간대 읽기가 응답하지 않으면 기다리지 않고 Asia/Seoul로 보낸다', () async {
    // given: 플랫폼 채널이 끝나지 않는 기기 (타임아웃은 테스트용으로 짧게)
    final deps = _onboardingDeps(
      timeZoneReader: () => Completer<String>().future,
      extraOverrides: [
        timeZoneReadTimeoutProvider.overrideWithValue(
          const Duration(milliseconds: 10),
        ),
      ],
    );
    final (_, controller) = await _ready(deps);

    // when
    await controller.submit();

    // then
    expect(_sentTimezone(deps), 'Asia/Seoul');
  });

  test('시간대가 서버가 모르는 약어로 오면 Asia/Seoul로 보낸다', () async {
    // given
    final deps = _onboardingDeps(timeZoneReader: () async => 'KST');
    final (_, controller) = await _ready(deps);

    // when
    await controller.submit();

    // then
    expect(_sentTimezone(deps), 'Asia/Seoul');
  });

  test('보내는 중에는 입력값을 바꿀 수 없다', () async {
    // given: 응답을 붙잡아 두어 보내는 중인 상태
    final deps = onboardingUser();
    final release = Completer<void>();
    deps.adapter.on('PUT', profilePath, (_) async {
      await release.future;
      return const FakeResponse(200, {'status': 'ACTIVE'});
    });
    final (container, controller) = await _ready(deps);
    final submitting = controller.submit();

    // when: 화면에서 화요일을 더 누른다
    controller.edit((d) => d.copyWith(days: {...d.days, DateTime.tuesday}));

    // then: 보낸 값(월·수·금)과 화면 값이 같아야 한다
    final days = container.read(onboardingControllerProvider).draft.days;
    release.complete();
    await submitting;
    expect(days, completeDraft.days);
  });

  test('앞 단계 입력이 비어 있으면 제출하지 않는다', () async {
    // given: 3단계는 채웠지만 키가 비어 있다 (설정 화면처럼 3단계부터 쓰는 경우)
    final deps = onboardingUser();
    final (_, controller) = await _ready(deps);
    controller.edit((d) => d.copyWith(heightText: ''));

    // when
    await controller.submit();

    // then
    expect(deps.adapter.count('PUT', profilePath), 0);
  });

  test('예상하지 못한 예외가 나면 문제가 생겼다는 실패를 알리고 다시 보낼 수 있다', () async {
    // given: AppException이 아닌 예외
    final deps = _onboardingDeps(
      extraOverrides: [profileApiProvider.overrideWithValue(_ThrowingApi())],
    );
    final (container, controller) = await _ready(deps);

    // when
    await controller.submit();

    // then
    final state = container.read(onboardingControllerProvider);
    expect((state.failure, state.submitting), (SubmitFailure.server, false));
  });
}
