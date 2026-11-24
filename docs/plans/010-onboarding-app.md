# 010 앱 온보딩 화면 구현 계획

- 명세: docs/specs/onboarding.md (확정 2026-11-15)
- 크기: L의 일부 (온보딩 중 2/2. 서버 009는 `feat/onboarding-server`)
- 상태: 승인됨 (2026-11-24)
- 브랜치: `feat/onboarding-app` (`feat/onboarding-server`에서 분기. 009 PR이 먼저 머지된다)

## Context

서버에는 온보딩 제출 API(`PUT /api/v1/users/me/profile`, 009)가 생겼지만, 앱은 아직 "온보딩 준비 중" 자리 화면과 건너뛰기 버튼만 갖고 있다. 건너뛰기는 앱 상태만 Active로 바꾸므로, 앱을 다시 켜면 다시 자리 화면이 나온다. 이번 작업에서 자리 화면을 프로토타입의 3단계 입력 화면으로 바꾸고, 제출이 성공하면 서버 상태가 ACTIVE가 되어 오늘 탭으로 가게 한다. 서버 코드는 바꾸지 않는다.

## 접근

- **기능 폴더 `features/onboarding/`을 새로 둔다** (data·domain·presentation). 지금 자리 화면은 `features/auth/presentation/`에 있지만, 온보딩은 인증과 별개의 기능이고 나중에 설정 화면(FR-04)이 같은 폼과 API를 다시 쓴다.
- **입력값은 불변 객체 `OnboardingDraft` 하나에 모은다.** 단계별 "다음" 가능 여부(`step1Valid` 등)와 서버 요청 본문 만들기(`toRequestJson`)를 이 객체가 맡는다. 순수 Dart라 단위 테스트가 쉽고, 검증 규칙이 서버 009와 같은지 한곳에서 비교할 수 있다.
- **상태는 `OnboardingController`(`Notifier`, autoDispose)가 갖는다.** 현재 단계, draft, 제출 상태(입력 중·보내는 중·실패 이유)를 가진다. 화면을 떠나면(로그아웃 등) 버려진다. 명세대로 입력값을 기기에 남기지 않는다.
- **제출이 성공하면 `AuthController.onboardingCompleted()`가 상태를 Active로 바꾸고, 라우터가 오늘 탭으로 보낸다.** 지금 `skipOnboarding()`을 이 메서드로 바꾼다. 화면이 직접 이동하지 않고 로그인 상태가 화면을 정하는 기존 방식(008)을 그대로 따른다.
- **"올해"와 시간대는 provider로 주입한다.** 나이 → 출생 연도 변환은 `clockProvider`(지금 시각)를, 시간대는 `deviceTimeZoneProvider`를 쓴다. 테스트에서 연말 경계(UTC와 한국의 연도가 다른 날)를 고정할 수 있다.
- **기기 시간대 이름(IANA, 예: `Asia/Seoul`)은 `flutter_timezone` 패키지로 읽는다.** Dart의 `DateTime.timeZoneName`은 `KST` 같은 약어라 서버 `ZoneId`가 해석하지 못한다. 읽기에 실패하면 `Asia/Seoul`을 보낸다 (확인 필요 1).

## 변경 파일

| 파일 | 변경 | 내용 |
| --- | --- | --- |
| app/lib/features/onboarding/domain/onboarding_draft.dart | 신규 | 입력값, 단계별 검증, 장비 프리셋 매핑, 요청 본문 만들기 |
| app/lib/features/onboarding/domain/onboarding_controller.dart | 신규 | 단계 이동, 값 변경, 제출(중복 방지), 실패 이유 |
| app/lib/features/onboarding/data/profile_api.dart | 신규 | `PUT /api/v1/users/me/profile` (`authDioProvider` 사용, `toAppException`) |
| app/lib/features/onboarding/data/device_time_zone.dart | 신규 | `deviceTimeZoneProvider` (flutter_timezone, 실패 시 `Asia/Seoul`) |
| app/lib/features/onboarding/presentation/onboarding_screen.dart | 신규 | 위쪽 단계 표시(1/3, 막대 3개)·이전, 아래 고정 버튼, 단계별 본문 교체 |
| app/lib/features/onboarding/presentation/steps/ (body_step, goal_step, schedule_step) | 신규 | 단계별 입력 UI |
| app/lib/core/widgets/choice_chips.dart | 신규 | 프로토타입 `.choices`: 하나 고르기·여러 개 고르기 알약 버튼 (설정 화면에서도 씀) |
| app/lib/core/widgets/unit_text_field.dart | 신규 | 숫자 입력 + 단위(cm·kg·세) |
| app/lib/core/time/clock.dart | 신규 | `clockProvider` (`DateTime Function()`, 기본 `DateTime.now`) |
| app/lib/features/auth/domain/auth_controller.dart | 수정 | `skipOnboarding()` → `onboardingCompleted()` |
| app/lib/router.dart | 수정 | `/onboarding` → `OnboardingScreen` |
| app/lib/features/auth/presentation/onboarding_placeholder_screen.dart | 삭제 | 자리 화면과 건너뛰기 제거 |
| app/pubspec.yaml | 수정 | `flutter_timezone` 추가 (받기 전 버전·라이선스 확인) |
| app/test/... | 신규·수정 | 아래 테스트 목록. 기존 router 테스트 2개(온보딩 준비 중, 건너뛰기)는 새 화면 기준으로 바꾼다 |
| CLAUDE.md | 수정 | 진행 상황, 앱 구조의 기능 목록 |

## 화면

프로토타입 `onboard()` 그대로, 명세에서 더한 항목(성별, 운동 시간, 직접 고르기)만 추가한다.

| 단계 | 제목 | 입력 | 처음 값 |
| --- | --- | --- | --- |
| 1 | 몸 정보를 알려주세요 | 키(cm), 몸무게(kg), 나이(세): `UnitTextField`, 성별: 남·여 칩 | 모두 비어 있음 (가짜 기본값을 그대로 제출하는 일을 막는다) |
| 2 | 어떤 몸을 만들고 싶나요? | 목표: 감량·근비대·유지 / 경력: 1년 미만·1~3년·3년 이상 / 장비: 헬스장 전체·덤벨만·맨몸·직접 고르기 (직접 고르기를 누르면 10종 여러 개 고르기가 펼쳐짐) | 선택 없음 |
| 3 | 언제 운동하나요? | 요일: 월~일 여러 개 / 분할: 전신·2분할·3분할 / 알림 시각: 시·분 휠(분은 5분 간격) / 운동 시간: 30·45·60·90분 | 요일·분할 없음, 19:00, 60분 |

- 아래 버튼: 1·2단계는 "다음", 3단계는 "오늘의 운동 받기". 단계 입력이 유효하지 않으면 비활성이다. 보내는 중에는 진행 표시를 보여 주고 누를 수 없다.
- 제출 실패 문구는 버튼 위에 보여 주고, 입력값은 그대로 둔다.
- 장비 10종 표시 이름: 바벨, 덤벨, 머신, 케이블, 벤치, 랙, 딥스 바, 턱걸이 바, 케틀벨, 맨몸.

## API

| 메서드 | 경로 | 요청 | 응답 | 앱이 분기하는 에러 |
| --- | --- | --- | --- | --- |
| PUT | /api/v1/users/me/profile | 명세 예시 형식 (`workoutDays`: `["MONDAY",…]`, `alarmTime`: `"19:00"`) | 200 (status ACTIVE). 앱은 본문을 쓰지 않는다 | `NetworkException` → 연결 확인 / `NOT_FOUND`(회원 없음) → 로그아웃 / 그 밖(`INVALID_INPUT`, `FORBIDDEN`, 500) → 문제가 생겼어요 |

## 핵심 코드 스케치

### 입력값과 검증 (`onboarding_draft.dart`)

```dart
enum EquipmentChoice { fullGym, dumbbellOnly, bodyweight, custom }

@immutable
class OnboardingDraft {
  const OnboardingDraft({this.heightText = '', this.weightText = '', this.ageText = '', this.sex,
      this.goal, this.experience, this.equipmentChoice, this.customEquipment = const {},
      this.days = const {}, this.split, this.alarm = const TimeOfDay(hour: 19, minute: 0),
      this.sessionMinutes = 60});

  // 서버 009와 같은 규칙 (키 100~250, 몸무게 30~300, 소수 한 자리, 나이 14~100)
  bool get step1Valid => _decimalIn(heightText, 100, 250) && _decimalIn(weightText, 30, 300)
      && _intIn(ageText, 14, 100) && sex != null;
  bool get step2Valid => goal != null && experience != null && equipmentChoice != null; // 직접 고르기 0개 허용
  bool get step3Valid => days.isNotEmpty && split != null;

  /// 프리셋 → 장비 목록 (명세 "프리셋 장비 매핑"). 서버는 프리셋을 모른다
  Set<Equipment> get equipment => switch (equipmentChoice) {
    EquipmentChoice.fullGym => Equipment.values.toSet(),
    EquipmentChoice.dumbbellOnly => {Equipment.dumbbell, Equipment.bench, Equipment.bodyweight},
    EquipmentChoice.bodyweight => {Equipment.bodyweight, Equipment.pullupBar},
    EquipmentChoice.custom => customEquipment,
    null => const {},
  };

  /// [now]는 사용자 기기 시각. 출생 연도 = 올해 − 나이
  Map<String, Object?> toRequestJson({required DateTime now, required String timeZone}) => {
    'heightCm': double.parse(heightText), 'weightKg': double.parse(weightText),
    'birthYear': now.toLocal().year - int.parse(ageText),
    'equipment': equipment.map((e) => e.wire).toList(),
    'workoutDays': (days.toList()..sort()).map(_dayWire).toList(), // DateTime.monday(1) … sunday(7) → MONDAY …
    'alarmTime': '${_two(alarm.hour)}:${_two(alarm.minute)}',
    'timezone': timeZone, 'sessionMinutes': sessionMinutes, ...
  };
}

// "175", "175.5"만 통과. "175.25", "1e2", "" 는 거절 (서버 @Digits(fraction=1)와 같게)
final _oneDecimal = RegExp(r'^\d{1,3}(\.\d)?$');
```

### 제출 (`onboarding_controller.dart`)

```dart
Future<void> submit() async {
  if (state.submitting || !state.draft.step3Valid) return;   // 보내는 중 다시 눌러도 한 번만
  state = state.copyWith(submitting: true, failure: null);
  try {
    final body = state.draft.toRequestJson(now: ref.read(clockProvider)(),
        timeZone: await ref.read(deviceTimeZoneProvider.future));
    await ref.read(profileApiProvider).submit(body);
    ref.read(authControllerProvider.notifier).onboardingCompleted(); // → Active → 라우터가 오늘 탭으로
  } on NetworkException {
    state = state.copyWith(submitting: false, failure: SubmitFailure.network);
  } on SessionExpiredException {
    // 인터셉터가 이미 토큰을 지우고 로그인 화면으로 보냈다
  } on ApiException catch (e) when (e.code == 'NOT_FOUND') {  // 토큰은 유효한데 회원이 없음
    await ref.read(authControllerProvider.notifier).signOut();
  } on AppException {
    state = state.copyWith(submitting: false, failure: SubmitFailure.server);
  }
}
```

- Dart의 `catch ... when` 대신 `on ApiException catch (e)` 안에서 `if`로 나눈다 (문법상 `when`이 없음).
- 성공 후에는 `submitting`을 되돌리지 않는다. 곧 화면이 바뀌므로 버튼을 다시 누를 수 없게 둔다.

### 시간대 (`device_time_zone.dart`)

```dart
final deviceTimeZoneProvider = FutureProvider<String>((ref) async {
  try {
    return (await FlutterTimezone.getLocalTimezone()).identifier; // 예: Asia/Seoul
  } on Object {
    return 'Asia/Seoul'; // 읽지 못해도 온보딩을 막지 않는다 (첫 사용자는 한국)
  }
});
```

## 정합성·동시성·멱등성

- **중복 제출:** 버튼 연타나 응답 전 재시도로 여러 번 보내도, 서버가 덮어쓰기라 결과가 같다(009). 앱도 `submitting` 동안은 버튼을 막아 요청을 한 번만 보낸다 (테스트 13).
- **응답을 못 받은 경우:** 서버는 저장했는데 연결이 끊겨 앱이 실패로 본 경우, 다시 보내면 같은 값으로 덮어써 200이 된다. 앱을 다시 켜도 시작 판정이 ACTIVE를 받아 오늘 탭으로 간다.
- **로그인 상태와 화면:** 화면 이동은 `AuthController` 상태 하나로만 정한다. 제출 성공과 세션 만료가 겹쳐도 라우터의 redirect 규칙(008)을 그대로 따른다.

## 실패·대체 경로

| 실패 | 동작 |
| --- | --- |
| 연결 실패·타임아웃 | 3단계에 남음, "연결을 확인해 주세요", 입력값 유지, 다시 누르면 재전송 |
| 세션 만료 (갱신 실패) | 인터셉터가 로그인 화면으로 보낸다 (기존) |
| `NOT_FOUND` (회원 없음) | 로그아웃 → 로그인 화면 |
| `INVALID_INPUT`·`FORBIDDEN`·500·형식 다른 응답 | "문제가 생겼어요. 잠시 후 다시 시도해 주세요", 입력값 유지 |
| 시간대 읽기 실패 | `Asia/Seoul`로 보낸다 |

## 테스트 목록

규칙은 008과 같다: `test`/`testWidgets` 한글 이름, given/when/then, mocktail, `TestDeps`(가짜 서버·메모리 토큰), `TZ=Asia/Seoul`. 시각과 시간대는 provider를 바꿔 끼운다.

단위: 입력값 (`onboarding_draft_test.dart`)
- [ ] 1. `키가 100cm 미만이거나 250cm를 넘으면 1단계를 넘어갈 수 없다`
- [ ] 2. `키나 몸무게에 소수 둘째 자리가 있으면 1단계를 넘어갈 수 없다`
- [ ] 3. `나이가 14세 미만이거나 100세를 넘으면 1단계를 넘어갈 수 없다`
- [ ] 4. `1단계를 모두 채우면 다음 단계로 넘어갈 수 있다`
- [ ] 5. `직접 고르기에서 장비를 하나도 고르지 않아도 2단계를 넘어갈 수 있다`
- [ ] 6. `운동 요일을 하나도 고르지 않으면 3단계를 마칠 수 없다`
- [ ] 7. `덤벨만 프리셋은 덤벨·벤치·맨몸으로 보낸다`
- [ ] 8. `한국 시각으로 새해가 된 순간에는 새해 기준으로 출생 연도를 계산한다` (시각 2026-12-31T15:30Z = 한국 2027-01-01, 나이 29 → 1998)
- [ ] 9. `요청 본문의 요일은 월요일부터 서버 이름으로, 알림 시각은 시:분으로 만든다`

단위: 제출 (`onboarding_controller_test.dart`)
- [ ] 10. `제출에 성공하면 로그인 상태가 active가 된다`
- [ ] 11. `제출 중 연결이 끊기면 입력값을 유지하고 연결 실패를 알린다`
- [ ] 12. `제출이 서버 오류면 문제가 생겼다는 실패를 알린다`
- [ ] 13. `보내는 중에 다시 제출해도 요청은 한 번만 보낸다`
- [ ] 14. `제출 결과가 회원 없음이면 로그아웃 상태가 된다`
- [ ] 15. `기기 시간대를 읽지 못하면 Asia/Seoul로 보낸다`

위젯 (`onboarding_screen_test.dart`, `router_test.dart`)
- [ ] 16. `ONBOARDING 회원이 로그인하면 온보딩 1단계 화면으로 간다` (기존 router 테스트 수정)
- [ ] 17. `1단계를 채우고 다음을 누르면 2단계로 간다`
- [ ] 18. `2단계에서 이전을 누르면 1단계로 돌아가고 입력한 키가 남아 있다`
- [ ] 19. `직접 고르기를 누르면 장비 10종 선택지가 보인다`
- [ ] 20. `세 단계를 마치고 오늘의 운동 받기를 누르면 오늘 탭으로 간다` (기존 "건너뛰기" 테스트를 대체)
- [ ] 21. `제출이 연결 실패면 3단계에 남아 연결을 확인하라는 문구를 보여준다`

수동 확인 (iOS 시뮬레이터, 로컬 서버를 `feat/onboarding-server`로 실행)
- [ ] 개발용 로그인 → 3단계 입력 → 오늘의 운동 받기 → 오늘 탭 → 앱 다시 켜도 온보딩이 나오지 않음
- [ ] DB `user_profile`·`user_equipment`·`users.status` 확인, 시간대가 `Asia/Seoul`로 저장됨
- [ ] 화면을 프로토타입·레퍼런스 색과 비교 (키보드가 입력칸·버튼을 가리지 않는지)

## 작업 순서

1. `feat/onboarding-server`에서 `feat/onboarding-app` 브랜치 → 계획 문서 첫 커밋
2. `flutter_timezone` 추가 (버전·라이선스·용량을 먼저 확인받음)
3. 시그니처(draft, controller, api, provider, 빈 화면) → 테스트 1~21 작성 → 실패 확인 → 보고
4. 구현: draft → api·controller → 공통 위젯 → 단계 화면 → 라우터·AuthController → 자리 화면 삭제
5. 시뮬레이터 수동 확인, 화면 다듬기
6. `/review-change` → 반영 → ai-log에 온보딩(009+010) 한 줄 기록, 중복된 008 행 정리 → 커밋. push·PR은 사용자

## 확인 필요

1. (해결) 기기 시간대는 `flutter_timezone`으로 읽는다. 받기 전에 버전·라이선스·용량을 확인받는다.

## 변경 이력
- 2026-11-24: 계획대로 승인. 기기 시간대는 `flutter_timezone`으로 읽기로 확정 (확인 필요 1)
