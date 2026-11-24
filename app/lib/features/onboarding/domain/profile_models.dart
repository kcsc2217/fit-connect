/// 온보딩 프로필 값. 서버와는 enum 이름(wire)으로 주고받는다 (명세 docs/specs/onboarding.md).
/// 화면에 보이는 한글 이름은 presentation이 정한다.
library;

enum Sex {
  male('MALE'),
  female('FEMALE');

  const Sex(this.wire);
  final String wire;
}

enum Goal {
  cut('CUT'),
  bulk('BULK'),
  maintain('MAINTAIN');

  const Goal(this.wire);
  final String wire;
}

enum Experience {
  beginner('BEGINNER'),
  intermediate('INTERMEDIATE'),
  advanced('ADVANCED');

  const Experience(this.wire);
  final String wire;
}

enum SplitType {
  fullBody('FULL_BODY'),
  twoSplit('TWO_SPLIT'),
  threeSplit('THREE_SPLIT');

  const SplitType(this.wire);
  final String wire;
}

/// 서버 common.type.Equipment와 같은 순서
enum Equipment {
  barbell('BARBELL'),
  dumbbell('DUMBBELL'),
  machine('MACHINE'),
  cable('CABLE'),
  bench('BENCH'),
  rack('RACK'),
  dipBar('DIP_BAR'),
  pullupBar('PULLUP_BAR'),
  kettlebell('KETTLEBELL'),
  bodyweight('BODYWEIGHT');

  const Equipment(this.wire);
  final String wire;
}

/// 장비 고르는 방법. 서버는 프리셋을 모르고 장비 목록만 받는다
enum EquipmentChoice { fullGym, dumbbellOnly, bodyweight, custom }

/// 운동 시간 선택지 (분). 서버 검증과 같다
const sessionMinuteOptions = [30, 45, 60, 90];

/// 알림 시각 (사용자 시간대 기준). 분은 5분 단위
class AlarmTime {
  const AlarmTime(this.hour, this.minute);

  final int hour;
  final int minute;

  @override
  bool operator ==(Object other) =>
      other is AlarmTime && other.hour == hour && other.minute == minute;

  @override
  int get hashCode => Object.hash(hour, minute);
}
