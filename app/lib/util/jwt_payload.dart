import 'dart:convert';

/// id_token 가운데 부분(payload)을 디코드한 값. 화면 표시용이라 서명은 검증하지 않는다 (검증은 서버 몫).
class IdTokenClaims {
  const IdTokenClaims(this.iss, this.aud);

  final String? iss;
  final List<String> aud;

  static IdTokenClaims? tryParse(String token) {
    final parts = token.split('.');
    if (parts.length != 3) return null;
    try {
      final payload = jsonDecode(utf8.decode(base64Url.decode(base64Url.normalize(parts[1]))));
      if (payload is! Map<String, dynamic>) return null;
      // JWT 표준상 aud는 문자열 하나 또는 배열이다
      final aud = switch (payload['aud']) {
        final String s => [s],
        final List<dynamic> l => l.cast<String>(),
        _ => <String>[],
      };
      return IdTokenClaims(payload['iss'] as String?, aud);
    } on FormatException {
      return null;
    }
  }
}
