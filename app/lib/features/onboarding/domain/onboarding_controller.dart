import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
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
}

/// 온보딩 단계 이동과 제출. 화면을 떠나면 버린다 (입력값을 기기에 남기지 않음, 명세)
class OnboardingController extends Notifier<OnboardingState> {
  @override
  OnboardingState build() => const OnboardingState();

  void edit(OnboardingDraft Function(OnboardingDraft) change) =>
      throw UnimplementedError('010 구현');

  /// 지금 단계 입력이 유효할 때만 다음 단계로
  void next() => throw UnimplementedError('010 구현');

  void back() => throw UnimplementedError('010 구현');

  /// 3단계 입력이 유효하고 보내는 중이 아닐 때만 보낸다. 성공하면 로그인 상태가 Active가 된다
  Future<void> submit() => throw UnimplementedError('010 구현');
}

final onboardingControllerProvider =
    NotifierProvider.autoDispose<OnboardingController, OnboardingState>(
      OnboardingController.new,
    );
