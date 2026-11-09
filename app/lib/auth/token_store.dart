import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// 우리 서버의 access·refresh 토큰 저장소
abstract interface class TokenStore {
  Future<void> save(String accessToken, String refreshToken);
  Future<String?> readAccess();
  Future<String?> readRefresh();
  Future<void> clear();
}

/// 기기 보안 저장소(안드로이드 Keystore로 암호화, iOS 키체인)에 둔다.
/// SharedPreferences는 암호화되지 않아 토큰을 두지 않는다.
class SecureTokenStore implements TokenStore {
  const SecureTokenStore([this._storage = const FlutterSecureStorage()]);

  final FlutterSecureStorage _storage;

  static const _accessKey = 'access_token';
  static const _refreshKey = 'refresh_token';

  @override
  Future<void> save(String accessToken, String refreshToken) async {
    await _storage.write(key: _accessKey, value: accessToken);
    await _storage.write(key: _refreshKey, value: refreshToken);
  }

  @override
  Future<String?> readAccess() => _storage.read(key: _accessKey);

  @override
  Future<String?> readRefresh() => _storage.read(key: _refreshKey);

  @override
  Future<void> clear() async {
    await _storage.delete(key: _accessKey);
    await _storage.delete(key: _refreshKey);
  }
}
