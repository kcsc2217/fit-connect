import 'dart:convert';

import 'package:http/http.dart' as http;

import 'api_exception.dart';
import 'models.dart';

/// 백엔드 인증 API 호출. http.Client를 주입받아 테스트에서 MockClient로 바꾼다.
class AuthApi {
  AuthApi(this._client, this._baseUrl);

  final http.Client _client;
  final String _baseUrl;

  static const _json = {'Content-Type': 'application/json'};

  /// provider: KAKAO | GOOGLE | DEV, token: id_token (DEV면 개발용 사용자 ID)
  Future<LoginResult> login(String provider, String token) async {
    final body = await _post('/api/v1/auth/login', {'provider': provider, 'token': token});
    return LoginResult.fromJson(body!);
  }

  Future<Me> me(String accessToken) async {
    final response = await _client.get(
      Uri.parse('$_baseUrl/api/v1/users/me'),
      headers: {'Authorization': 'Bearer $accessToken'},
    );
    return Me.fromJson(_decode(response)!);
  }

  Future<TokenPair> refresh(String refreshToken) async {
    final body = await _post('/api/v1/auth/refresh', {'refreshToken': refreshToken});
    return TokenPair.fromJson(body!);
  }

  Future<void> logout(String refreshToken) async {
    await _post('/api/v1/auth/logout', {'refreshToken': refreshToken});
  }

  Future<Map<String, dynamic>?> _post(String path, Map<String, String> body) async {
    final response = await _client.post(
      Uri.parse('$_baseUrl$path'),
      headers: _json,
      body: jsonEncode(body),
    );
    return _decode(response);
  }

  /// 2xx면 본문(204면 null), 아니면 ApiException
  Map<String, dynamic>? _decode(http.Response response) {
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException.fromResponse(response);
    }
    if (response.bodyBytes.isEmpty) return null;
    return jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
  }
}
