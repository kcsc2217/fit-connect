import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:trainer_ssaem/features/auth/data/auth_api.dart';
import 'package:trainer_ssaem/features/auth/data/token_storage.dart';

/// access를 갱신한다. 동시에 여러 번 불려도 /auth/refresh는 한 번만 보낸다.
/// refresh는 한 번만 쓸 수 있어서(docs/decisions/0004), 두 번 보내면 두 번째가 실패하고
/// 멀쩡한 사용자가 로그아웃되기 때문이다.
class TokenRefresher {
  TokenRefresher(this._authApi, this._storage);

  final AuthApi _authApi;
  final TokenStorage _storage;

  /// 새 access를 돌려준다.
  /// - 갱신 실패(refresh 없음, 401): SessionExpiredException
  /// - 연결 실패: NetworkException (토큰을 지우지 않는다)
  Future<String> refresh() => throw UnimplementedError();

  // 스텁 단계에서 필드 미사용 경고를 피하기 위한 접근 (구현 시 제거)
  Object get deps => (_authApi, _storage);
}

final tokenRefresherProvider = Provider<TokenRefresher>(
  (ref) => TokenRefresher(
    ref.watch(authApiProvider),
    ref.watch(tokenStorageProvider),
  ),
);
