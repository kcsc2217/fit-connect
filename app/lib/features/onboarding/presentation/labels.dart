import 'package:trainer_ssaem/features/onboarding/domain/profile_models.dart';

/// 온보딩 값의 화면 이름 (프로토타입 문구 + 명세 결정)
const sexOptions = [(Sex.male, '남'), (Sex.female, '여')];

const goalOptions = [
  (Goal.cut, '감량'),
  (Goal.bulk, '근비대'),
  (Goal.maintain, '유지'),
];

const experienceOptions = [
  (Experience.beginner, '1년 미만'),
  (Experience.intermediate, '1~3년'),
  (Experience.advanced, '3년 이상'),
];

const equipmentChoiceOptions = [
  (EquipmentChoice.fullGym, '헬스장 전체'),
  (EquipmentChoice.dumbbellOnly, '덤벨만'),
  (EquipmentChoice.bodyweight, '맨몸'),
  (EquipmentChoice.custom, '직접 고르기'),
];

const equipmentOptions = [
  (Equipment.barbell, '바벨'),
  (Equipment.dumbbell, '덤벨'),
  (Equipment.machine, '머신'),
  (Equipment.cable, '케이블'),
  (Equipment.bench, '벤치'),
  (Equipment.rack, '랙'),
  (Equipment.dipBar, '딥스 바'),
  (Equipment.pullupBar, '턱걸이 바'),
  (Equipment.kettlebell, '케틀벨'),
  (Equipment.bodyweight, '맨몸'),
];

/// DateTime.monday(1) … DateTime.sunday(7)
const dayOptions = [
  (DateTime.monday, '월'),
  (DateTime.tuesday, '화'),
  (DateTime.wednesday, '수'),
  (DateTime.thursday, '목'),
  (DateTime.friday, '금'),
  (DateTime.saturday, '토'),
  (DateTime.sunday, '일'),
];

const splitOptions = [
  (SplitType.fullBody, '전신'),
  (SplitType.twoSplit, '2분할'),
  (SplitType.threeSplit, '3분할'),
];
