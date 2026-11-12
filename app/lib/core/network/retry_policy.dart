import 'package:trainer_ssaem/core/network/api_exception.dart';

/// Riverpod 3은 실패한 provider를 기본으로 최대 10번(합계 약 38초) 자동 재시도한다.
/// 서버·연결 오류(AppException)는 화면의 "다시 시도"로 사용자에게 맡기고 바로 실패시킨다.
/// 그 밖의 예상하지 못한 오류(Keychain 오류 등)만 짧게 두 번 다시 해 본다.
/// 앱(main.dart)과 테스트(TestDeps)가 같은 정책을 쓴다.
Duration? appRetry(int retryCount, Object error) {
  if (error is AppException) return null;
  if (retryCount >= 2) return null;
  return Duration(milliseconds: 200 * (1 << retryCount));
}
