import 'package:flutter/foundation.dart';
import 'package:trainer_ssaem/features/onboarding/domain/profile_models.dart';

/// 온보딩 입력값. 단계별 검증과 서버 요청 본문 만들기를 맡는다 (계획 010).
/// 검증 규칙은 서버 009와 같다: 키 100~250cm, 몸무게 30~300kg(소수 한 자리), 나이 14~100세.
@immutable
class OnboardingDraft {
  const OnboardingDraft({
    this.heightText = '',
    this.weightText = '',
    this.ageText = '',
    this.sex,
    this.goal,
    this.experience,
    this.equipmentChoice,
    this.customEquipment = const {},
    this.days = const {},
    this.split,
    this.alarm = const AlarmTime(19, 0),
    this.sessionMinutes = 60,
  });

  final String heightText;
  final String weightText;
  final String ageText;
  final Sex? sex;
  final Goal? goal;
  final Experience? experience;
  final EquipmentChoice? equipmentChoice;

  /// "직접 고르기"에서 고른 장비
  final Set<Equipment> customEquipment;

  /// 운동 요일. DateTime.monday(1) … DateTime.sunday(7)
  final Set<int> days;
  final SplitType? split;
  final AlarmTime alarm;
  final int sessionMinutes;

  bool get step1Valid => throw UnimplementedError('010 구현');

  /// 직접 고르기에서 장비를 하나도 고르지 않아도 된다 (맨몸 종목만 후보)
  bool get step2Valid => throw UnimplementedError('010 구현');

  bool get step3Valid => throw UnimplementedError('010 구현');

  /// 프리셋을 장비 목록으로 (명세 "프리셋 장비 매핑")
  Set<Equipment> get equipment => throw UnimplementedError('010 구현');

  /// [now]는 기기 시각. 출생 연도 = 기기 시간대 기준 올해 − 나이
  Map<String, Object?> toRequestJson({
    required DateTime now,
    required String timeZone,
  }) => throw UnimplementedError('010 구현');

  OnboardingDraft copyWith({
    String? heightText,
    String? weightText,
    String? ageText,
    Sex? sex,
    Goal? goal,
    Experience? experience,
    EquipmentChoice? equipmentChoice,
    Set<Equipment>? customEquipment,
    Set<int>? days,
    SplitType? split,
    AlarmTime? alarm,
    int? sessionMinutes,
  }) => OnboardingDraft(
    heightText: heightText ?? this.heightText,
    weightText: weightText ?? this.weightText,
    ageText: ageText ?? this.ageText,
    sex: sex ?? this.sex,
    goal: goal ?? this.goal,
    experience: experience ?? this.experience,
    equipmentChoice: equipmentChoice ?? this.equipmentChoice,
    customEquipment: customEquipment ?? this.customEquipment,
    days: days ?? this.days,
    split: split ?? this.split,
    alarm: alarm ?? this.alarm,
    sessionMinutes: sessionMinutes ?? this.sessionMinutes,
  );
}
