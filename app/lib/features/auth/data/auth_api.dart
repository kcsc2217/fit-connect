import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:trainer_ssaem/core/network/api_exception.dart';
import 'package:trainer_ssaem/core/network/http_client.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';

/// 로그인 응답: 토큰 + 회원 상태 (상태로 온보딩 화면 여부를 정한다)
class LoginResult {
  const LoginResult({required this.tokens, required this.status});

  final AuthTokens tokens;
  final UserStatus status;
}

/// 서버 인증 API. 인증 헤더가 없는 Dio를 쓴다. 실패는 AppException으로 던진다.
class AuthApi {
  AuthApi(this._dio);

  final Dio _dio;

  /// POST /api/v1/auth/login
  Future<LoginResult> login(AuthProvider provider, String token) async {
    final body = await _post('/api/v1/auth/login', {
      'provider': provider.wire,
      'token': token,
    });
    return LoginResult(
      tokens: AuthTokens.fromJson(body!),
      status: UserStatus.fromWire(body['status'] as String),
    );
  }

  /// POST /api/v1/auth/refresh
  Future<AuthTokens> refresh(String refreshToken) async {
    final body = await _post('/api/v1/auth/refresh', {
      'refreshToken': refreshToken,
    });
    return AuthTokens.fromJson(body!);
  }

  /// POST /api/v1/auth/logout
  Future<void> logout(String refreshToken) async {
    await _post('/api/v1/auth/logout', {'refreshToken': refreshToken});
  }

  Future<Map<String, dynamic>?> _post(
    String path,
    Map<String, String> data,
  ) async {
    try {
      final res = await _dio.post<Map<String, dynamic>>(path, data: data);
      return res.data;
    } on DioException catch (e) {
      throw toAppException(e);
    }
  }
}

final authApiProvider = Provider<AuthApi>(
  (ref) => AuthApi(ref.watch(plainDioProvider)),
);
