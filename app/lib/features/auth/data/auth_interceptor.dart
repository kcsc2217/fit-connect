import 'package:dio/dio.dart';
import 'package:trainer_ssaem/core/network/api_exception.dart';
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

  static const _retriedKey = 'auth_retried';

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final access = await _storage.readAccessToken();
    if (access != null) options.headers['Authorization'] = 'Bearer $access';
    handler.next(options);
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    if (err.response?.statusCode != 401) return handler.next(err);

    final request = err.requestOptions;
    // 다시 보낸 요청도 401이거나, 만료가 아닌 401(UNAUTHORIZED 등)이면 갱신하지 않는다
    if (request.extra[_retriedKey] == true ||
        problemCodeOf(err.response) != 'TOKEN_EXPIRED') {
      return handler.reject(await _expireSession(request));
    }

    try {
      final access = await _freshAccess(request);
      final retry = request
        ..headers['Authorization'] = 'Bearer $access'
        ..extra[_retriedKey] = true;
      return handler.resolve(await _dio.fetch<Object?>(retry));
    } on SessionExpiredException {
      return handler.reject(await _expireSession(request));
    } on NetworkException catch (e) {
      return handler.reject(_wrap(request, e)); // 토큰 유지
    } on DioException catch (e) {
      // 재요청이 실패했다. 재요청의 onError가 이미 처리했으므로 그대로 넘긴다
      return handler.reject(e);
    }
  }

  /// 이 요청이 보낸 access보다 새것이 이미 저장돼 있으면(다른 요청이 갱신함) 그것을 쓰고,
  /// 아니면 갱신한다.
  Future<String> _freshAccess(RequestOptions request) async {
    final sent = (request.headers['Authorization'] as String?)?.replaceFirst(
      'Bearer ',
      '',
    );
    final stored = await _storage.readAccessToken();
    if (stored != null && stored != sent) return stored;
    return _refresher.refresh();
  }

  Future<DioException> _expireSession(RequestOptions request) async {
    await _storage.clear();
    await _onSessionExpired();
    return _wrap(request, const SessionExpiredException());
  }

  /// 부르는 쪽이 toAppException으로 [error]를 그대로 꺼낼 수 있게 감싼다
  DioException _wrap(RequestOptions request, AppException error) =>
      DioException(
        requestOptions: request,
        type: DioExceptionType.unknown,
        error: error,
      );
}
