import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';

/// 토큰 보관소. 앱에서는 iOS Keychain, 테스트에서는 메모리.
abstract interface class TokenStorage {
  Future<AuthTokens?> read();
  Future<String?> readAccessToken();
  Future<String?> readRefreshToken();
  Future<void> save(AuthTokens tokens);
  Future<void> clear();
}

/// Keychain(flutter_secure_storage)에 토큰 4개를 둔다. 일반 저장소·로그에는 남기지 않는다.
class SecureTokenStorage implements TokenStorage {
  SecureTokenStorage([FlutterSecureStorage? storage])
    : _storage = storage ?? const FlutterSecureStorage();

  final FlutterSecureStorage _storage;

  @override
  Future<AuthTokens?> read() => throw UnimplementedError();

  @override
  Future<String?> readAccessToken() => throw UnimplementedError();

  @override
  Future<String?> readRefreshToken() => throw UnimplementedError();

  @override
  Future<void> save(AuthTokens tokens) => throw UnimplementedError();

  @override
  Future<void> clear() => throw UnimplementedError();

  // 스텁 단계에서 필드 미사용 경고를 피하기 위한 접근 (구현 시 제거)
  FlutterSecureStorage get raw => _storage;
}

final tokenStorageProvider = Provider<TokenStorage>(
  (ref) => SecureTokenStorage(),
);
