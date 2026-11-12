import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:trainer_ssaem/core/network/api_exception.dart';
import 'package:trainer_ssaem/features/auth/data/auth_api.dart';
import 'package:trainer_ssaem/features/auth/data/auth_interceptor.dart';
import 'package:trainer_ssaem/features/auth/data/token_refresher.dart';

import '../../support/fake_http_adapter.dart';
import '../../support/fixtures.dart';
import '../../support/memory_token_storage.dart';

/// 인터셉터를 단 Dio와 가짜 서버·보관소 묶음
class _Harness {
  _Harness() {
    final plain = Dio(BaseOptions(baseUrl: 'http://test'))
      ..httpClientAdapter = adapter;
    final refresher = TokenRefresher(AuthApi(plain), storage);
    dio = Dio(BaseOptions(baseUrl: 'http://test'))..httpClientAdapter = adapter;
    dio.interceptors.add(
      AuthInterceptor(
        dio: dio,
        storage: storage,
        refresher: refresher,
        onSessionExpired: () async => sessionExpiredCalls++,
      ),
    );
  }

  final adapter = FakeHttpAdapter();
  final storage = MemoryTokenStorage(tokens('old-access', 'old-refresh'));
  late final Dio dio;
  int sessionExpiredCalls = 0;

  /// 요청 결과를 성공(응답 본문) 또는 AppException으로 돌려준다
  Future<Object?> getMe() async {
    try {
      return (await dio.get<Object?>(mePath)).data;
    } on DioException catch (e) {
      return toAppException(e);
    }
  }

  /// old-access면 만료, new-access면 성공으로 답하는 내 정보 API
  void meExpiresOldAccess() {
    adapter.on(
      'GET',
      mePath,
      (req) => bearerOf(req) == 'new-access'
          ? FakeResponse(200, meJson())
          : FakeResponse.problem(401, 'TOKEN_EXPIRED'),
    );
  }

  /// 갱신 성공 (new-access, new-refresh)
  void refreshSucceeds({Duration delay = Duration.zero}) {
    adapter.on('POST', refreshPath, (req) async {
      await Future<void>.delayed(delay);
      return FakeResponse(200, tokenJson('new-access', 'new-refresh'));
    });
  }
}

void main() {
  test('401 TOKEN_EXPIRED를 받으면 갱신한 뒤 새 access로 원래 요청을 다시 보낸다', () async {
    // given
    final h = _Harness()
      ..meExpiresOldAccess()
      ..refreshSucceeds();

    // when
    final result = await h.getMe();

    // then
    expect(result, meJson());
  });

  test('동시에 세 요청이 TOKEN_EXPIRED를 받아도 갱신 요청은 한 번만 보낸다', () async {
    // given
    final h = _Harness()
      ..meExpiresOldAccess()
      ..refreshSucceeds(delay: const Duration(milliseconds: 20));

    // when
    await Future.wait([h.getMe(), h.getMe(), h.getMe()]);

    // then
    expect(h.adapter.count('POST', refreshPath), 1);
  });

  test('다른 요청이 이미 갱신했으면 갱신 없이 저장된 새 access로 다시 보낸다', () async {
    // given
    final h = _Harness()..refreshSucceeds();
    h.adapter.on('GET', mePath, (req) {
      if (bearerOf(req) == 'new-access') return FakeResponse(200, meJson());
      // 이 요청이 old-access로 나간 뒤, 다른 요청의 갱신이 먼저 끝난 상황
      h.storage.tokens = tokens('new-access', 'new-refresh');
      return FakeResponse.problem(401, 'TOKEN_EXPIRED');
    });

    // when
    await h.getMe();

    // then
    expect(h.adapter.count('POST', refreshPath), 0);
  });

  test('갱신이 INVALID_REFRESH_TOKEN으로 실패하면 토큰을 지우고 세션 만료를 알린다', () async {
    // given
    final h = _Harness()..meExpiresOldAccess();
    h.adapter.on(
      'POST',
      refreshPath,
      (_) => FakeResponse.problem(401, 'INVALID_REFRESH_TOKEN'),
    );

    // when
    final result = await h.getMe();

    // then
    expect(
      (
        result is SessionExpiredException,
        h.storage.tokens,
        h.sessionExpiredCalls,
      ),
      (true, null, 1),
    );
  });

  test('401 UNAUTHORIZED를 받으면 갱신하지 않고 토큰을 지운다', () async {
    // given
    final h = _Harness()..refreshSucceeds();
    h.adapter.on(
      'GET',
      mePath,
      (_) => FakeResponse.problem(401, 'UNAUTHORIZED'),
    );

    // when
    await h.getMe();

    // then
    expect((h.adapter.count('POST', refreshPath), h.storage.tokens), (0, null));
  });

  test('갱신 후 다시 보낸 요청도 401이면 다시 갱신하지 않는다', () async {
    // given
    final h = _Harness()..refreshSucceeds();
    h.adapter.on(
      'GET',
      mePath,
      (_) => FakeResponse.problem(401, 'TOKEN_EXPIRED'),
    );

    // when
    await h.getMe();

    // then
    expect(h.adapter.count('POST', refreshPath), 1);
  });

  test('갱신 중 네트워크가 끊기면 토큰을 지우지 않는다', () async {
    // given
    final h = _Harness()..meExpiresOldAccess();
    h.adapter.on('POST', refreshPath, (req) => throw offline(req));

    // when
    final result = await h.getMe();

    // then
    expect(
      (result is NetworkException, h.storage.tokens?.refreshToken),
      (true, 'old-refresh'),
    );
  });
}
