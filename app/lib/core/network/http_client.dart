import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:trainer_ssaem/core/config/app_config.dart';

/// 테스트에서 가짜 HTTP 어댑터로 바꿔 끼우는 자리. 앱에서는 null (Dio 기본 어댑터).
final httpClientAdapterProvider = Provider<HttpClientAdapter?>((ref) => null);

/// 서버 주소·타임아웃이 설정된 새 Dio를 만든다.
Dio createDio(Ref ref) {
  final dio = Dio(
    BaseOptions(
      baseUrl: ref.watch(appConfigProvider).apiBaseUrl,
      connectTimeout: const Duration(seconds: 5),
      receiveTimeout: const Duration(seconds: 10),
      contentType: Headers.jsonContentType,
    ),
  );
  final adapter = ref.watch(httpClientAdapterProvider);
  if (adapter != null) dio.httpClientAdapter = adapter;
  return dio;
}

/// 인증 헤더를 붙이지 않는 Dio. 로그인·갱신·로그아웃에 쓴다 (갱신이 인터셉터를 다시 타지 않게).
final plainDioProvider = Provider<Dio>(createDio);
