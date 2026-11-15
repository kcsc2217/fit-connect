import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:trainer_ssaem/core/network/api_exception.dart';

final _request = RequestOptions(path: '/api/v1/users/me');

DioException _badResponse(int status, Object? body) => DioException.badResponse(
  statusCode: status,
  requestOptions: _request,
  response: Response<Object?>(
    requestOptions: _request,
    statusCode: status,
    data: body,
  ),
);

void main() {
  test('ProblemDetail 응답이면 status와 code를 읽어 ApiException으로 바꾼다', () {
    // given
    final error = _badResponse(503, {
      'title': 'Service Unavailable',
      'status': 503,
      'detail': '소셜 로그인 서버에 연결할 수 없습니다',
      'code': 'SOCIAL_LOGIN_UNAVAILABLE',
    });

    // when
    final result = toAppException(error);

    // then
    expect(result is ApiException ? (result.status, result.code) : result, (
      503,
      'SOCIAL_LOGIN_UNAVAILABLE',
    ));
  });

  test('code가 없는 응답이면 UnknownApiException으로 바꾼다', () {
    // given
    final error = _badResponse(502, '<html>Bad Gateway</html>');

    // when
    final result = toAppException(error);

    // then
    expect(result, isA<UnknownApiException>());
  });

  test('연결 실패와 타임아웃은 NetworkException으로 바꾼다', () {
    // given
    final errors = [
      DioException.connectionError(requestOptions: _request, reason: 'offline'),
      DioException.connectionTimeout(
        requestOptions: _request,
        timeout: const Duration(seconds: 5),
      ),
      DioException.receiveTimeout(
        requestOptions: _request,
        timeout: const Duration(seconds: 10),
      ),
    ];

    // when
    final results = errors.map(toAppException).toList();

    // then
    expect(results, everyElement(isA<NetworkException>()));
  });
}
