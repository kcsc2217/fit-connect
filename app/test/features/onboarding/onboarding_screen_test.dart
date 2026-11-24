import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';

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
    expect(labels.where((l) => find.text(l).evaluate().isEmpty), isEmpty);
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
      find.descendant(of: find.byKey(const ValueKey('height')), matching: find.byType(EditableText)),
    );
    expect(field.focusNode.hasFocus, isFalse);
  });
}
