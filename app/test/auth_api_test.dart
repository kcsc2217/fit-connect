import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:traffic_gym_app/api/api_exception.dart';
import 'package:traffic_gym_app/api/auth_api.dart';

http.Response jsonResponse(Object body, int status) => http.Response.bytes(
      utf8.encode(jsonEncode(body)),
      status,
      headers: {'content-type': 'application/json'},
    );

void main() {
  const baseUrl = 'http://server';

  test('로그인 200 응답을 LoginResult로 읽는다', () async {
    // given
    final api = AuthApi(
      MockClient((req) async => jsonResponse({
            'accessToken': 'a1',
            'accessTokenExpiresAt': '2026-11-09T00:30:00Z',
            'refreshToken': 'r1',
            'refreshTokenExpiresAt': '2026-11-23T00:00:00Z',
            'status': 'ONBOARDING',
          }, 200)),
      baseUrl,
    );

    // when
    final result = await api.login('DEV', 'dev-user');

    // then
    expect(
      [result.accessToken, result.refreshToken, result.status],
      ['a1', 'r1', 'ONBOARDING'],
    );
  });

  test('401 ProblemDetail에서 INVALID_SOCIAL_TOKEN 코드를 읽는다', () async {
    // given
    final api = AuthApi(
      MockClient((req) async => jsonResponse({
            'type': 'about:blank',
            'title': 'Unauthorized',
            'status': 401,
            'detail': '소셜 로그인 정보를 확인할 수 없습니다',
            'code': 'INVALID_SOCIAL_TOKEN',
          }, 401)),
      baseUrl,
    );

    // when
    final future = api.login('KAKAO', 'bad-token');

    // then
    await expectLater(
      future,
      throwsA(isA<ApiException>()
          .having((e) => e.status, 'status', 401)
          .having((e) => e.code, 'code', 'INVALID_SOCIAL_TOKEN')),
    );
  });

  test('본문이 JSON이 아니면 코드는 null이고 상태 코드만 남는다', () async {
    // given
    final api = AuthApi(
      MockClient((req) async => http.Response('<html>Bad Gateway</html>', 502)),
      baseUrl,
    );

    // when
    final future = api.login('DEV', 'dev-user');

    // then
    await expectLater(
      future,
      throwsA(isA<ApiException>()
          .having((e) => e.status, 'status', 502)
          .having((e) => e.code, 'code', isNull)),
    );
  });

  test('내 정보 요청에 Bearer 헤더를 붙인다', () async {
    // given
    String? authHeader;
    final api = AuthApi(
      MockClient((req) async {
        authHeader = req.headers['Authorization'];
        return jsonResponse({
          'id': 1,
          'nickname': '트레이너',
          'status': 'ACTIVE',
          'createdAt': '2026-11-09T00:00:00Z',
          'providers': ['KAKAO'],
        }, 200);
      }),
      baseUrl,
    );

    // when
    await api.me('access-1');

    // then
    expect(authHeader, 'Bearer access-1');
  });
}
