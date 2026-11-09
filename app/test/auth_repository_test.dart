import 'package:flutter_test/flutter_test.dart';
import 'package:traffic_gym_app/api/api_exception.dart';
import 'package:traffic_gym_app/api/auth_api.dart';
import 'package:traffic_gym_app/api/models.dart';
import 'package:traffic_gym_app/auth/auth_repository.dart';
import 'package:traffic_gym_app/auth/token_store.dart';

/// 테스트용 메모리 저장소 (실제 앱은 SecureTokenStore)
class MemoryTokenStore implements TokenStore {
  String? access;
  String? refresh;

  @override
  Future<void> save(String accessToken, String refreshToken) async {
    access = accessToken;
    refresh = refreshToken;
  }

  @override
  Future<String?> readAccess() async => access;

  @override
  Future<String?> readRefresh() async => refresh;

  @override
  Future<void> clear() async {
    access = null;
    refresh = null;
  }
}

/// 서버 대역. 정해 둔 응답을 돌려주고 호출을 기록한다.
class FakeAuthApi implements AuthApi {
  final List<String> calls = [];
  final Map<String, Object> meResults = {}; // access → Me 또는 ApiException
  Object refreshResult = const TokenPair('new-access', 'new-refresh');
  Object logoutResult = 'ok';

  static Me sampleMe(String nickname) =>
      Me(1, nickname, 'ACTIVE', DateTime.utc(2026, 11, 9), const ['DEV']);

  @override
  Future<LoginResult> login(String provider, String token) async {
    calls.add('login:$provider');
    return const LoginResult('access-1', 'refresh-1', 'ACTIVE');
  }

  @override
  Future<Me> me(String accessToken) async {
    calls.add('me:$accessToken');
    final result = meResults[accessToken]!;
    if (result is ApiException) throw result;
    return result as Me;
  }

  @override
  Future<TokenPair> refresh(String refreshToken) async {
    calls.add('refresh:$refreshToken');
    final result = refreshResult;
    if (result is ApiException) throw result;
    return result as TokenPair;
  }

  @override
  Future<void> logout(String refreshToken) async {
    calls.add('logout:$refreshToken');
    final result = logoutResult;
    if (result is ApiException) throw result;
  }
}

void main() {
  late MemoryTokenStore store;
  late FakeAuthApi api;
  late AuthRepository repository;

  setUp(() {
    store = MemoryTokenStore();
    api = FakeAuthApi();
    repository = AuthRepository(api, store);
  });

  test('로그인하면 토큰을 저장하고 내 정보를 돌려준다', () async {
    // given
    api.meResults['access-1'] = FakeAuthApi.sampleMe('나');

    // when
    final me = await repository.loginAndFetchMe('DEV', 'dev-user');

    // then
    expect([me.nickname, store.access, store.refresh], ['나', 'access-1', 'refresh-1']);
  });

  test('TOKEN_EXPIRED면 갱신 후 새 토큰을 저장하고 한 번 재시도한다', () async {
    // given
    await store.save('old-access', 'old-refresh');
    api.meResults['old-access'] = const ApiException(401, 'TOKEN_EXPIRED', null);
    api.meResults['new-access'] = FakeAuthApi.sampleMe('갱신됨');

    // when
    final me = await repository.fetchMe();

    // then
    expect(
      [me.nickname, store.access, store.refresh, ...api.calls],
      ['갱신됨', 'new-access', 'new-refresh', 'me:old-access', 'refresh:old-refresh', 'me:new-access'],
    );
  });

  test('UNAUTHORIZED면 갱신하지 않고 에러를 그대로 던진다', () async {
    // given
    await store.save('bad-access', 'refresh-1');
    api.meResults['bad-access'] = const ApiException(401, 'UNAUTHORIZED', null);

    // when
    final future = repository.fetchMe();

    // then
    await expectLater(future, throwsA(isA<ApiException>().having((e) => e.code, 'code', 'UNAUTHORIZED')));
    expect(api.calls, ['me:bad-access']);
  });

  test('갱신이 실패하면 저장소를 비운다', () async {
    // given
    await store.save('old-access', 'used-refresh');
    api.meResults['old-access'] = const ApiException(401, 'TOKEN_EXPIRED', null);
    api.refreshResult = const ApiException(401, 'INVALID_REFRESH_TOKEN', null);

    // when
    await expectLater(repository.fetchMe(), throwsA(isA<ApiException>()));

    // then
    expect([store.access, store.refresh], [null, null]);
  });

  test('로그아웃하면 서버 호출이 실패해도 저장소를 비운다', () async {
    // given
    await store.save('access-1', 'refresh-1');
    api.logoutResult = const ApiException(503, null, null);

    // when
    await repository.logout();

    // then
    expect([store.access, store.refresh], [null, null]);
  });
}
