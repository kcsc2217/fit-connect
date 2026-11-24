import 'package:flutter_test/flutter_test.dart';
import 'package:trainer_ssaem/features/onboarding/domain/profile_models.dart';

import '../../support/onboarding_steps.dart';

void main() {
  test('키가 100cm 미만이거나 250cm를 넘으면 1단계를 넘어갈 수 없다', () {
    // given
    final tooShort = completeDraft.copyWith(heightText: '99.9');
    final tooTall = completeDraft.copyWith(heightText: '250.1');

    // when
    final results = (tooShort.step1Valid, tooTall.step1Valid);

    // then
    expect(results, (false, false));
  });

  test('키나 몸무게에 소수 둘째 자리가 있으면 1단계를 넘어갈 수 없다', () {
    // given
    final height = completeDraft.copyWith(heightText: '175.25');
    final weight = completeDraft.copyWith(weightText: '72.55');

    // when
    final results = (height.step1Valid, weight.step1Valid);

    // then
    expect(results, (false, false));
  });

  test('나이가 14세 미만이거나 100세를 넘으면 1단계를 넘어갈 수 없다', () {
    // given
    final young = completeDraft.copyWith(ageText: '13');
    final old = completeDraft.copyWith(ageText: '101');

    // when
    final results = (young.step1Valid, old.step1Valid);

    // then
    expect(results, (false, false));
  });

  test('1단계를 모두 채우면 다음 단계로 넘어갈 수 있다', () {
    // given: 경계값 (키 100, 몸무게 300, 나이 14)
    final draft = completeDraft.copyWith(
      heightText: '100',
      weightText: '300.0',
      ageText: '14',
    );

    // when
    final valid = draft.step1Valid;

    // then
    expect(valid, isTrue);
  });

  test('직접 고르기에서 장비를 하나도 고르지 않아도 2단계를 넘어갈 수 있다', () {
    // given
    final draft = completeDraft.copyWith(
      equipmentChoice: EquipmentChoice.custom,
      customEquipment: {},
    );

    // when
    final valid = draft.step2Valid;

    // then
    expect(valid, isTrue);
  });

  test('운동 요일을 하나도 고르지 않으면 3단계를 마칠 수 없다', () {
    // given
    final draft = completeDraft.copyWith(days: {});

    // when
    final valid = draft.step3Valid;

    // then
    expect(valid, isFalse);
  });

  test('덤벨만 프리셋은 덤벨·벤치·맨몸으로 보낸다', () {
    // given
    final draft = completeDraft.copyWith(
      equipmentChoice: EquipmentChoice.dumbbellOnly,
    );

    // when
    final equipment = draft.equipment;

    // then
    expect(equipment, {
      Equipment.dumbbell,
      Equipment.bench,
      Equipment.bodyweight,
    });
  });

  test('한국 시각으로 새해가 된 순간에는 새해 기준으로 출생 연도를 계산한다', () {
    // given: UTC로는 2026-12-31 15:30, 한국(테스트 TZ)으로는 2027-01-01 00:30
    final now = DateTime.utc(2026, 12, 31, 15, 30);

    // when
    final body = completeDraft.toRequestJson(now: now, timeZone: 'Asia/Seoul');

    // then: 2027 - 29
    expect(body['birthYear'], 1998);
  });

  test('요청 본문의 요일은 월요일부터 서버 이름으로, 알림 시각은 시:분으로 만든다', () {
    // given
    final draft = completeDraft.copyWith(
      days: {DateTime.friday, DateTime.monday, DateTime.sunday},
      alarm: const AlarmTime(7, 5),
    );

    // when
    final body = draft.toRequestJson(
      now: DateTime.utc(2026, 11, 24),
      timeZone: 'Asia/Seoul',
    );

    // then
    expect(
      (body['workoutDays'], body['alarmTime']),
      (['MONDAY', 'FRIDAY', 'SUNDAY'], '07:05'),
    );
  });
}
