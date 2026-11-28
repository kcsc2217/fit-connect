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

  bool get heightValid => _decimalIn(heightText, 100, 250);
  bool get weightValid => _decimalIn(weightText, 30, 300);
  bool get ageValid => _intIn(ageText, 14, 100);

  bool get step1Valid => heightValid && weightValid && ageValid && sex != null;

  /// 직접 고르기에서 장비를 하나도 고르지 않아도 된다 (맨몸 종목만 후보)
  bool get step2Valid =>
      goal != null && experience != null && equipmentChoice != null;

  bool get step3Valid => days.isNotEmpty && split != null;

  /// 프리셋을 장비 목록으로 (명세 "프리셋 장비 매핑")
  Set<Equipment> get equipment => switch (equipmentChoice) {
    EquipmentChoice.fullGym => Equipment.values.toSet(),
    EquipmentChoice.dumbbellOnly => const {
      Equipment.dumbbell,
      Equipment.bench,
      Equipment.bodyweight,
    },
    EquipmentChoice.bodyweight => const {
      Equipment.bodyweight,
      Equipment.pullupBar,
    },
    EquipmentChoice.custom => customEquipment,
    null => const {},
  };

  /// [now]는 기기 시각. 출생 연도 = 기기 시계·시간대 기준 올해 − 나이 ([timeZone] 값은 쓰지 않는다).
  /// 서버는 [timeZone]과 서버 시계로 올해를 정하므로 연말·연초 몇 초 사이에는 어긋날 수 있다 (명세 알려진 한계).
  /// 세 단계가 모두 유효할 때만 부른다
  Map<String, Object?> toRequestJson({
    required DateTime now,
    required String timeZone,
  }) => {
    'heightCm': double.parse(heightText),
    'weightKg': double.parse(weightText),
    'birthYear': now.toLocal().year - int.parse(ageText),
    'sex': sex!.wire,
    'goal': goal!.wire,
    'experience': experience!.wire,
    'equipment': (equipment.toList()..sort((a, b) => a.index - b.index))
        .map((e) => e.wire)
        .toList(),
    'workoutDays': (days.toList()..sort())
        .map((d) => _dayWires[d - 1])
        .toList(),
    'splitType': split!.wire,
    'alarmTime': '${_two(alarm.hour)}:${_two(alarm.minute)}',
    'timezone': timeZone,
    'sessionMinutes': sessionMinutes,
  };

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

// "175", "175.5"만 통과. "175.25", "1e2", ""는 거절 (서버 @Digits(fraction=1)와 같게)
final _oneDecimal = RegExp(r'^\d{1,3}(\.\d)?$');
final _wholeNumber = RegExp(r'^\d{1,3}$');

bool _decimalIn(String text, num min, num max) {
  if (!_oneDecimal.hasMatch(text)) return false;
  final value = double.parse(text);
  return value >= min && value <= max;
}

bool _intIn(String text, int min, int max) {
  if (!_wholeNumber.hasMatch(text)) return false;
  final value = int.parse(text);
  return value >= min && value <= max;
}

String _two(int n) => n.toString().padLeft(2, '0');

// DateTime.monday(1) … DateTime.sunday(7) → 서버 DayOfWeek 이름
const _dayWires = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];
