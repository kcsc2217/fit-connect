import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:trainer_ssaem/core/network/api_exception.dart';
import 'package:trainer_ssaem/core/time/clock.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_controller.dart';
import 'package:trainer_ssaem/features/onboarding/data/device_time_zone.dart';
import 'package:trainer_ssaem/features/onboarding/data/profile_api.dart';
import 'package:trainer_ssaem/features/onboarding/domain/onboarding_draft.dart';

/// 제출 실패 이유 (화면 문구를 나눈다)
enum SubmitFailure {
  /// 연결 실패·타임아웃
  network,

  /// 서버 오류, 형식이 다른 응답, 예상하지 못한 에러 코드
  server,
}

@immutable
class OnboardingState {
  const OnboardingState({
    this.step = 0,
    this.draft = const OnboardingDraft(),
    this.submitting = false,
    this.failure,
  });

  /// 0, 1, 2 (화면에는 1 / 3처럼 보인다)
  final int step;
  final OnboardingDraft draft;
  final bool submitting;
  final SubmitFailure? failure;

  static const lastStep = 2;

  OnboardingState copyWith({
    int? step,
    OnboardingDraft? draft,
    bool? submitting,
    SubmitFailure? failure,
    bool clearFailure = false,
  }) => OnboardingState(
    step: step ?? this.step,
    draft: draft ?? this.draft,
    submitting: submitting ?? this.submitting,
    failure: clearFailure ? null : (failure ?? this.failure),
  );
}

/// 온보딩 단계 이동과 제출. 화면을 떠나면 버린다 (입력값을 기기에 남기지 않음, 명세)
class OnboardingController extends Notifier<OnboardingState> {
  @override
  OnboardingState build() => const OnboardingState();

  void edit(OnboardingDraft Function(OnboardingDraft) change) {
    // 값을 고치면 지난 제출 실패 문구는 지운다
    state = state.copyWith(draft: change(state.draft), clearFailure: true);
  }

  bool get currentStepValid => switch (state.step) {
    0 => state.draft.step1Valid,
    1 => state.draft.step2Valid,
    _ => state.draft.step3Valid,
  };

  /// 지금 단계 입력이 유효할 때만 다음 단계로
  void next() {
    if (state.step < OnboardingState.lastStep && currentStepValid) {
      state = state.copyWith(step: state.step + 1);
    }
  }

  void back() {
    if (state.step > 0 && !state.submitting) {
      state = state.copyWith(step: state.step - 1, clearFailure: true);
    }
  }

  /// 3단계 입력이 유효하고 보내는 중이 아닐 때만 보낸다. 성공하면 로그인 상태가 Active가 된다
  Future<void> submit() async {
    // 보내는 중 다시 눌러도 한 번만
    if (state.submitting || !state.draft.step3Valid) return;
    state = state.copyWith(submitting: true, clearFailure: true);
    try {
      final body = state.draft.toRequestJson(
        now: ref.read(clockProvider)(),
        timeZone: await ref.read(deviceTimeZoneProvider.future),
      );
      await ref.read(profileApiProvider).submit(body);
      // Active가 되면 라우터가 오늘 탭으로 보낸다. 화면이 바뀌므로 submitting은 그대로 둔다
      ref.read(authControllerProvider.notifier).onboardingCompleted();
    } on NetworkException {
      state = state.copyWith(submitting: false, failure: SubmitFailure.network);
    } on SessionExpiredException {
      // 인터셉터가 이미 토큰을 지우고 로그인 화면으로 보냈다
    } on ApiException catch (e) {
      if (e.code == 'NOT_FOUND') {
        // 토큰은 유효한데 회원이 서버에 없다 (지워진 회원). 다시 보내도 같으므로 로그아웃한다
        await ref.read(authControllerProvider.notifier).signOut();
        return;
      }
      state = state.copyWith(submitting: false, failure: SubmitFailure.server);
    } on AppException {
      state = state.copyWith(submitting: false, failure: SubmitFailure.server);
    }
  }
}

final onboardingControllerProvider =
    NotifierProvider.autoDispose<OnboardingController, OnboardingState>(
      OnboardingController.new,
    );
