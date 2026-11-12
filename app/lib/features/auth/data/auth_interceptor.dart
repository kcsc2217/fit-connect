import 'package:dio/dio.dart';
import 'package:trainer_ssaem/features/auth/data/token_refresher.dart';
import 'package:trainer_ssaem/features/auth/data/token_storage.dart';

/// 인증이 필요한 요청에 access를 붙이고, 만료되면 갱신 후 한 번 다시 보낸다.
/// - 401 TOKEN_EXPIRED: 갱신 → 재요청 1회 (다른 요청이 이미 갱신했으면 갱신 없이 재요청)
/// - 그 밖의 401, 갱신 실패, 재요청도 401: 토큰 삭제 → [onSessionExpired] → SessionExpiredException
/// - 갱신 중 연결 실패: 토큰 유지 → NetworkException
class AuthInterceptor extends Interceptor {
  // 비공개 필드를 이름 있는 인자로 받는다 (부르는 쪽 이름은 dio, storage, ...)
  AuthInterceptor({
    required this._dio,
    required this._storage,
    required this._refresher,
    required this._onSessionExpired,
  });

  final Dio _dio;
  final TokenStorage _storage;
  final TokenRefresher _refresher;
  final Future<void> Function() _onSessionExpired;

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    handler.next(err);
  }

  // 스텁 단계에서 필드 미사용 경고를 피하기 위한 접근 (구현 시 제거)
  Object get deps => (_dio, _storage, _refresher, _onSessionExpired);
}
