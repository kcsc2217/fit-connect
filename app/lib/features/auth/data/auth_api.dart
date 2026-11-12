import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
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
  Future<LoginResult> login(AuthProvider provider, String token) =>
      throw UnimplementedError();

  /// POST /api/v1/auth/refresh
  Future<AuthTokens> refresh(String refreshToken) => throw UnimplementedError();

  /// POST /api/v1/auth/logout
  Future<void> logout(String refreshToken) => throw UnimplementedError();

  // 스텁 단계에서 필드 미사용 경고를 피하기 위한 접근 (구현 시 제거)
  Dio get dio => _dio;
}

final authApiProvider = Provider<AuthApi>(
  (ref) => AuthApi(ref.watch(plainDioProvider)),
);
