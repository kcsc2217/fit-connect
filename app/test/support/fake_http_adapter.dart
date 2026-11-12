import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';

/// 가짜 서버 응답
class FakeResponse {
  const FakeResponse(this.status, [this.body, this.problem = false]);

  /// 서버 에러 형식 (ProblemDetail + code, docs/decisions/0003)
  factory FakeResponse.problem(int status, String code) => FakeResponse(
    status,
    {'type': 'about:blank', 'title': code, 'status': status, 'code': code},
    true,
  );

  final int status;
  final Object? body;
  final bool problem;
}

typedef FakeHandler = FutureOr<FakeResponse> Function(RequestOptions request);

/// Dio의 실제 네트워크 대신 쓰는 가짜 서버. 경로별 응답을 정하고, 받은 요청을 기록한다.
/// 연결 실패는 핸들러에서 [offline]을 던져 흉내 낸다.
class FakeHttpAdapter implements HttpClientAdapter {
  final _handlers = <String, FakeHandler>{};
  final requests = <RequestOptions>[];

  void on(String method, String path, FakeHandler handler) {
    _handlers['$method $path'] = handler;
  }

  /// 해당 경로로 온 요청 수
  int count(String method, String path) =>
      requests.where((r) => r.method == method && r.path == path).length;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(options);
    final handler = _handlers['${options.method} ${options.path}'];
    if (handler == null) {
      throw StateError('가짜 서버에 없는 경로: ${options.method} ${options.path}');
    }
    final res = await handler(options);
    return ResponseBody.fromString(
      res.body == null ? '' : jsonEncode(res.body),
      res.status,
      headers: {
        Headers.contentTypeHeader: [
          res.problem ? 'application/problem+json' : 'application/json',
        ],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

/// 연결 실패
DioException offline(RequestOptions request) =>
    DioException.connectionError(requestOptions: request, reason: 'offline');

/// 요청에 실린 access (Bearer 뒤의 값)
String? bearerOf(RequestOptions request) =>
    (request.headers['Authorization'] as String?)?.replaceFirst('Bearer ', '');
