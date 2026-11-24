import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:trainer_ssaem/features/onboarding/domain/onboarding_draft.dart';
import 'package:trainer_ssaem/features/onboarding/domain/profile_models.dart';

import 'fake_http_adapter.dart';
import 'fixtures.dart';
import 'memory_token_storage.dart';
import 'test_app.dart';

/// 세 단계를 모두 채운 입력값
const completeDraft = OnboardingDraft(
  heightText: '175',
  weightText: '72.5',
  ageText: '29',
  sex: Sex.male,
  goal: Goal.bulk,
  experience: Experience.intermediate,
  equipmentChoice: EquipmentChoice.fullGym,
  days: {DateTime.monday, DateTime.wednesday, DateTime.friday},
  split: SplitType.threeSplit,
);

/// 로그인된 ONBOARDING 회원. 프로필 제출은 성공
TestDeps onboardingUser() {
  final deps = TestDeps(storage: MemoryTokenStorage(tokens('a', 'r')));
  deps.adapter.on(
    'GET',
    mePath,
    (_) => FakeResponse(200, meJson(status: 'ONBOARDING')),
  );
  deps.adapter.on(
    'PUT',
    profilePath,
    (_) => const FakeResponse(200, {'status': 'ACTIVE'}),
  );
  deps.adapter.on('POST', logoutPath, (_) => const FakeResponse(204));
  return deps;
}

/// 화면 밖에 있어도 보이게 스크롤한 뒤 누른다
Future<void> tapText(WidgetTester tester, String text) async {
  final finder = find.text(text);
  await tester.ensureVisible(finder);
  await tester.tap(finder);
  await tester.pumpAndSettle();
}

Future<void> fillStep1(WidgetTester tester) async {
  await tester.enterText(find.byKey(const ValueKey('height')), '175');
  await tester.enterText(find.byKey(const ValueKey('weight')), '72.5');
  await tester.enterText(find.byKey(const ValueKey('age')), '29');
  await tapText(tester, '남');
}

Future<void> fillStep2(WidgetTester tester) async {
  await tapText(tester, '근비대');
  await tapText(tester, '1~3년');
  await tapText(tester, '헬스장 전체');
}

Future<void> fillStep3(WidgetTester tester) async {
  await tapText(tester, '월');
  await tapText(tester, '3분할');
}
