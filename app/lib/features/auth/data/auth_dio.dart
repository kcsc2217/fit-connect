import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:trainer_ssaem/core/network/http_client.dart';
import 'package:trainer_ssaem/features/auth/data/auth_interceptor.dart';
import 'package:trainer_ssaem/features/auth/data/token_refresher.dart';
import 'package:trainer_ssaem/features/auth/data/token_storage.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_controller.dart';

/// 인증이 필요한 API용 Dio. 다른 기능(me, 이후 workout 등)은 이 Dio로 서버를 부른다.
final authDioProvider = Provider<Dio>((ref) {
  final dio = createDio(ref);
  dio.interceptors.add(
    AuthInterceptor(
      dio: dio,
      storage: ref.watch(tokenStorageProvider),
      refresher: ref.watch(tokenRefresherProvider),
      // 호출 시점에 읽는다 (authController가 이 Dio를 쓰므로 만들 때 읽으면 순환)
      onSessionExpired: () async =>
          ref.read(authControllerProvider.notifier).sessionExpired(),
    ),
  );
  return dio;
});
