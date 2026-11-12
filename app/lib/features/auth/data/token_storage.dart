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

  static const _access = 'access_token';
  static const _accessExpiresAt = 'access_token_expires_at';
  static const _refresh = 'refresh_token';
  static const _refreshExpiresAt = 'refresh_token_expires_at';

  @override
  Future<AuthTokens?> read() async {
    final values = await _storage.readAll();
    final access = values[_access];
    final accessExpiresAt = DateTime.tryParse(values[_accessExpiresAt] ?? '');
    final refresh = values[_refresh];
    final refreshExpiresAt = DateTime.tryParse(values[_refreshExpiresAt] ?? '');
    if (access == null ||
        accessExpiresAt == null ||
        refresh == null ||
        refreshExpiresAt == null) {
      return null;
    }
    return AuthTokens(
      accessToken: access,
      accessTokenExpiresAt: accessExpiresAt,
      refreshToken: refresh,
      refreshTokenExpiresAt: refreshExpiresAt,
    );
  }

  @override
  Future<String?> readAccessToken() => _storage.read(key: _access);

  @override
  Future<String?> readRefreshToken() => _storage.read(key: _refresh);

  @override
  Future<void> save(AuthTokens tokens) async {
    await _storage.write(key: _access, value: tokens.accessToken);
    await _storage.write(
      key: _accessExpiresAt,
      value: tokens.accessTokenExpiresAt.toIso8601String(),
    );
    await _storage.write(key: _refresh, value: tokens.refreshToken);
    await _storage.write(
      key: _refreshExpiresAt,
      value: tokens.refreshTokenExpiresAt.toIso8601String(),
    );
  }

  @override
  Future<void> clear() async {
    for (final key in [
      _access,
      _accessExpiresAt,
      _refresh,
      _refreshExpiresAt,
    ]) {
      await _storage.delete(key: key);
    }
  }
}

final tokenStorageProvider = Provider<TokenStorage>(
  (ref) => SecureTokenStorage(),
);
