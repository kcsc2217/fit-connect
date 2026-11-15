import 'dart:convert';

import 'package:dio/dio.dart';

/// 앱 안에서 다루는 서버 호출 실패. 화면은 이 타입과 [ApiException.code]로 분기한다.
sealed class AppException implements Exception {
  const AppException();
}

/// 서버가 ProblemDetail + code로 답한 실패 (docs/decisions/0003).
class ApiException extends AppException {
  const ApiException({required this.status, required this.code, this.detail});

  final int status;

  /// 서버 ErrorCode 이름 (예: TOKEN_EXPIRED). 문구(detail)가 아니라 이 값으로 분기한다.
  final String code;
  final String? detail;

  @override
  String toString() => 'ApiException($status, $code)';
}

/// code가 없거나 형식이 다른 응답 (500 포함).
class UnknownApiException extends AppException {
  const UnknownApiException({this.status});

  final int? status;

  @override
  String toString() => 'UnknownApiException($status)';
}

/// 연결 실패, 타임아웃. 토큰을 지우지 않는다.
class NetworkException extends AppException {
  const NetworkException();

  @override
  String toString() => 'NetworkException';
}

/// 로그인이 풀렸다 (갱신 실패, UNAUTHORIZED). 토큰은 이미 지워진 상태다.
class SessionExpiredException extends AppException {
  const SessionExpiredException();

  @override
  String toString() => 'SessionExpiredException';
}

/// 응답 본문(ProblemDetail)에서 code를 꺼낸다. 없으면 null.
String? problemCodeOf(Response<dynamic>? response) {
  final body = _jsonBodyOf(response);
  final code = body?['code'];
  return code is String && code.isNotEmpty ? code : null;
}

/// Dio 예외를 [AppException]으로 바꾼다.
AppException toAppException(DioException error) {
  // 인터셉터가 이미 정한 결과 (세션 만료, 갱신 중 연결 실패)
  final inner = error.error;
  if (inner is AppException) return inner;

  switch (error.type) {
    case DioExceptionType.connectionTimeout:
    case DioExceptionType.sendTimeout:
    case DioExceptionType.receiveTimeout:
    case DioExceptionType.transformTimeout:
    case DioExceptionType.connectionError:
      return const NetworkException();
    case DioExceptionType.badResponse:
      final response = error.response;
      final status = response?.statusCode;
      final code = problemCodeOf(response);
      if (status == null || code == null) {
        return UnknownApiException(status: status);
      }
      final detail = _jsonBodyOf(response)?['detail'];
      return ApiException(
        status: status,
        code: code,
        detail: detail is String ? detail : null,
      );
    case DioExceptionType.badCertificate:
    case DioExceptionType.cancel:
    case DioExceptionType.unknown:
      return UnknownApiException(status: error.response?.statusCode);
  }
}

/// 응답 본문을 JSON 객체로 읽는다. 문자열로 온 경우(Content-Type이 JSON이 아닐 때)도 시도한다.
Map<String, dynamic>? _jsonBodyOf(Response<dynamic>? response) {
  final data = response?.data;
  if (data is Map<String, dynamic>) return data;
  if (data is String && data.trimLeft().startsWith('{')) {
    try {
      final decoded = jsonDecode(data);
      if (decoded is Map<String, dynamic>) return decoded;
    } on FormatException {
      return null;
    }
  }
  return null;
}
