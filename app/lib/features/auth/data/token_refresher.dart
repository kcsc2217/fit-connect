import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:trainer_ssaem/core/network/api_exception.dart';
import 'package:trainer_ssaem/features/auth/data/auth_api.dart';
import 'package:trainer_ssaem/features/auth/data/token_storage.dart';

/// access를 갱신한다. 동시에 여러 번 불려도 /auth/refresh는 한 번만 보낸다.
/// refresh는 한 번만 쓸 수 있어서(docs/decisions/0004), 두 번 보내면 두 번째가 실패하고
/// 멀쩡한 사용자가 로그아웃되기 때문이다.
class TokenRefresher {
  TokenRefresher(this._authApi, this._storage);

  final AuthApi _authApi;
  final TokenStorage _storage;

  /// 진행 중인 갱신. 앱 프로세스 안의 값이다 (Java의 CompletableFuture를 여럿이 같이 기다리는 것과 같다).
  Future<String>? _inFlight;

  /// 새 access를 돌려준다.
  /// - 갱신 실패(refresh 없음, 401): SessionExpiredException
  /// - 연결 실패: NetworkException (토큰을 지우지 않는다)
  Future<String> refresh() {
    return _inFlight ??= _doRefresh().whenComplete(() => _inFlight = null);
  }

  Future<String> _doRefresh() async {
    // 매번 보관소에서 읽는다: 직전에 다른 갱신이 끝났다면 교체된 최신 refresh를 쓴다
    final refreshToken = await _storage.readRefreshToken();
    if (refreshToken == null) throw const SessionExpiredException();
    try {
      final tokens = await _authApi.refresh(refreshToken);
      await _storage.save(tokens);
      return tokens.accessToken;
    } on ApiException catch (e) {
      if (e.status == 401) throw const SessionExpiredException();
      rethrow;
    }
  }
}

final tokenRefresherProvider = Provider<TokenRefresher>(
  (ref) => TokenRefresher(
    ref.watch(authApiProvider),
    ref.watch(tokenStorageProvider),
  ),
);
