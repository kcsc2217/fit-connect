import 'dart:convert';

import 'package:http/http.dart' as http;

/// 서버 에러 응답(ProblemDetail + code, 백엔드 ADR 0003).
/// 화면 분기는 detail 문구가 아니라 code로 한다.
class ApiException implements Exception {
  const ApiException(this.status, this.code, this.detail);

  final int status;
  final String? code;
  final String? detail;

  factory ApiException.fromResponse(http.Response response) {
    String? code;
    String? detail;
    try {
      // 한글이 깨지지 않게 body 대신 bodyBytes를 UTF-8로 읽는다
      final body = jsonDecode(utf8.decode(response.bodyBytes));
      if (body is Map<String, dynamic>) {
        code = body['code'] as String?;
        detail = body['detail'] as String?;
      }
    } on FormatException {
      // 본문이 JSON이 아니면(프록시 오류 페이지 등) 상태 코드만 남긴다
    }
    return ApiException(response.statusCode, code, detail);
  }

  @override
  String toString() => 'ApiException($status, $code, $detail)';
}
