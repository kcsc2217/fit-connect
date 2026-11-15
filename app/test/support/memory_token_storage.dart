import 'package:trainer_ssaem/features/auth/data/token_storage.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';

/// Keychain 대신 쓰는 메모리 보관소
class MemoryTokenStorage implements TokenStorage {
  MemoryTokenStorage([this.tokens]);

  AuthTokens? tokens;

  @override
  int generation = 0;

  @override
  Future<AuthTokens?> read() async => tokens;

  @override
  Future<String?> readAccessToken() async => tokens?.accessToken;

  @override
  Future<String?> readRefreshToken() async => tokens?.refreshToken;

  @override
  Future<void> save(AuthTokens tokens) async => this.tokens = tokens;

  @override
  Future<void> clear() async {
    generation++;
    tokens = null;
  }
}
