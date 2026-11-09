import '../api/api_exception.dart';
import '../api/auth_api.dart';
import '../api/models.dart';
import 'token_store.dart';

class NotLoggedInException implements Exception {
  const NotLoggedInException();
}

/// 로그인 → 토큰 저장 → 내 정보 조회, 만료 시 갱신, 로그아웃
class AuthRepository {
  AuthRepository(this._api, this._store);

  final AuthApi _api;
  final TokenStore _store;

  Future<Me> loginAndFetchMe(String provider, String token) async {
    final result = await _api.login(provider, token);
    await _store.save(result.accessToken, result.refreshToken);
    return fetchMe();
  }

  /// 401 code가 TOKEN_EXPIRED일 때만 refresh로 한 번 갱신하고 다시 조회한다 (백엔드 ADR 0004).
  /// 화면 하나라 요청이 동시에 나가지 않으므로 갱신을 하나로 묶는 처리(single-flight)는 하지 않는다.
  Future<Me> fetchMe() async {
    final access = await _store.readAccess();
    if (access == null) throw const NotLoggedInException();
    try {
      return await _api.me(access);
    } on ApiException catch (e) {
      // UNAUTHORIZED(서명 오류 등)는 갱신해도 소용없다
      if (e.code != 'TOKEN_EXPIRED') rethrow;
      final refresh = await _store.readRefresh();
      if (refresh == null) rethrow;

      final TokenPair renewed;
      try {
        renewed = await _api.refresh(refresh);
      } on ApiException {
        // INVALID_REFRESH_TOKEN 등: 다시 로그인해야 한다
        await _store.clear();
        rethrow;
      }
      // refresh는 쓸 때마다 교체되고 같은 값을 두 번 쓰면 401이라 받은 즉시 저장한다
      await _store.save(renewed.accessToken, renewed.refreshToken);
      return _api.me(renewed.accessToken);
    }
  }

  /// 서버 로그아웃이 실패해도(네트워크 등) 기기의 토큰은 지운다
  Future<void> logout() async {
    final refresh = await _store.readRefresh();
    try {
      if (refresh != null) await _api.logout(refresh);
    } on ApiException {
      // 서버 쪽 refresh는 만료되면 사라진다
    } finally {
      await _store.clear();
    }
  }
}
