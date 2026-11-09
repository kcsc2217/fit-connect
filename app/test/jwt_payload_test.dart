import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:traffic_gym_app/util/jwt_payload.dart';

/// 서명은 보지 않으므로 아무 문자열로 채운다
String fakeJwt(Map<String, Object> payload) {
  String part(Object json) => base64Url.encode(utf8.encode(jsonEncode(json))).replaceAll('=', '');
  return '${part({'alg': 'RS256'})}.${part(payload)}.signature';
}

void main() {
  test('aud가 문자열이면 한 개짜리 목록으로 읽는다', () {
    // given
    final token = fakeJwt({'iss': 'https://kauth.kakao.com', 'aud': 'kakao-app-key', 'sub': '123'});

    // when
    final claims = IdTokenClaims.tryParse(token);

    // then
    expect([claims!.iss, claims.aud], ['https://kauth.kakao.com', ['kakao-app-key']]);
  });

  test('aud가 배열이면 그대로 목록으로 읽는다', () {
    // given
    final token = fakeJwt({'iss': 'https://accounts.google.com', 'aud': ['a', 'b'], 'sub': '1'});

    // when
    final claims = IdTokenClaims.tryParse(token);

    // then
    expect(claims!.aud, ['a', 'b']);
  });

  test('형식이 잘못된 토큰은 null을 돌려준다', () {
    // given
    const token = 'not-a-jwt';

    // when
    final claims = IdTokenClaims.tryParse(token);

    // then
    expect(claims, isNull);
  });
}
