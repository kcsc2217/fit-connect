import 'dart:async';

import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:trainer_ssaem/features/onboarding/domain/onboarding_controller.dart';
import 'package:trainer_ssaem/features/onboarding/presentation/onboarding_screen.dart';

import '../../support/fake_http_adapter.dart';
import '../../support/fixtures.dart';
import '../../support/onboarding_steps.dart';
import '../../support/test_app.dart';

void main() {
  testWidgets('1단계를 채우고 다음을 누르면 2단계로 간다', (tester) async {
    // given
    await pumpTestApp(tester, onboardingUser());
    await fillStep1(tester);

    // when
    await tapText(tester, '다음');

    // then
    expect(find.text('어떤 몸을 만들고 싶나요?'), findsOneWidget);
  });

  testWidgets('2단계에서 이전을 누르면 1단계로 돌아가고 입력한 키가 남아 있다', (tester) async {
    // given
    await pumpTestApp(tester, onboardingUser());
    await fillStep1(tester);
    await tapText(tester, '다음');

    // when
    await tester.tap(find.byTooltip('이전'));
    await tester.pumpAndSettle();

    // then
    expect(find.text('175'), findsOneWidget);
  });

  testWidgets('직접 고르기를 누르면 장비 10종 선택지가 보인다', (tester) async {
    // given
    await pumpTestApp(tester, onboardingUser());
    await fillStep1(tester);
    await tapText(tester, '다음');

    // when
    await tapText(tester, '직접 고르기');

    // then
    const labels = [
      '바벨',
      '덤벨',
      '머신',
      '케이블',
      '벤치',
      '랙',
      '딥스 바',
      '턱걸이 바',
      '케틀벨',
      '맨몸',
    ];
    // '맨몸'은 프리셋 칩 이름이기도 해서 목록이 펼쳐졌다면 두 개 보여야 한다
    expect(
      labels.where((l) => find.text(l).evaluate().length < (l == '맨몸' ? 2 : 1)),
      isEmpty,
    );
  });

  testWidgets('세 단계를 마치고 오늘의 운동 받기를 누르면 오늘 탭으로 간다', (tester) async {
    // given
    await pumpTestApp(tester, onboardingUser());
    await fillStep1(tester);
    await tapText(tester, '다음');
    await fillStep2(tester);
    await tapText(tester, '다음');
    await fillStep3(tester);

    // when
    await tapText(tester, '오늘의 운동 받기');

    // then
    expect(find.text('오늘의 운동'), findsOneWidget);
  });

  testWidgets('제출이 연결 실패면 3단계에 남아 연결을 확인하라는 문구를 보여준다', (tester) async {
    // given
    final deps = onboardingUser();
    deps.adapter.on('PUT', profilePath, (req) => throw offline(req));
    await pumpTestApp(tester, deps);
    await fillStep1(tester);
    await tapText(tester, '다음');
    await fillStep2(tester);
    await tapText(tester, '다음');
    await fillStep3(tester);

    // when
    await tapText(tester, '오늘의 운동 받기');

    // then
    expect(find.text('연결을 확인해 주세요'), findsOneWidget);
  });

  testWidgets('2단계 내용은 가운데가 아니라 화면 왼쪽 여백에서 시작한다', (tester) async {
    // given: 입력칸이 없는 단계는 내용 폭이 좁아 가운데로 몰릴 수 있다
    await pumpTestApp(tester, onboardingUser());
    await fillStep1(tester);

    // when
    await tapText(tester, '다음');

    // then: 화면 좌우 여백 24
    expect(tester.getTopLeft(find.text('목표')).dx, 24);
  });

  testWidgets('입력칸 밖을 누르면 키보드가 닫힌다', (tester) async {
    // given: 숫자 키패드에는 닫기 키가 없다
    await pumpTestApp(tester, onboardingUser());
    await tester.tap(find.byKey(const ValueKey('height')));
    await tester.pump();

    // when
    await tester.tap(find.text('몸 정보를 알려주세요'));
    await tester.pump();

    // then
    final field = tester.widget<EditableText>(
      find.descendant(
        of: find.byKey(const ValueKey('height')),
        matching: find.byType(EditableText),
      ),
    );
    expect(field.focusNode.hasFocus, isFalse);
  });

  testWidgets('몸무게에 쉼표를 입력하면 소수점으로 바꿔 받는다', (tester) async {
    // given: 소수점이 쉼표인 지역의 키패드
    await pumpTestApp(tester, onboardingUser());

    // when
    await tester.enterText(find.byKey(const ValueKey('weight')), '72,5');
    await tester.pump();

    // then
    expect(find.text('72.5'), findsOneWidget);
  });

  testWidgets('보내는 중에는 3단계 선택지를 누를 수 없다', (tester) async {
    // given: 응답을 붙잡아 두어 보내는 중인 상태
    final deps = onboardingUser();
    final release = Completer<void>();
    deps.adapter.on('PUT', profilePath, (_) async {
      await release.future;
      return const FakeResponse(200, {'status': 'ACTIVE'});
    });
    await pumpTestApp(tester, deps);
    await fillStep1(tester);
    await tapText(tester, '다음');
    await fillStep2(tester);
    await tapText(tester, '다음');
    await fillStep3(tester);
    await tester.tap(find.text('오늘의 운동 받기'));
    await tester.pump();

    // when
    await tester.tap(find.text('화'), warnIfMissed: false);
    await tester.pump();

    // then: 화요일이 입력값에 들어가지 않았다 (응답을 풀기 전에 읽는다. 성공하면 화면이 바뀐다)
    final container = ProviderScope.containerOf(
      tester.element(find.byType(OnboardingScreen)),
    );
    final days = container.read(onboardingControllerProvider).draft.days;
    release.complete();
    await tester.pumpAndSettle();
    expect(days.contains(DateTime.tuesday), isFalse);
  });
}
