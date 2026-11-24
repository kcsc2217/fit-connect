import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:trainer_ssaem/core/network/api_exception.dart';
import 'package:trainer_ssaem/features/auth/data/auth_dio.dart';

/// 온보딩 프로필 API (서버 009)
class ProfileApi {
  ProfileApi(this._dio);

  final Dio _dio;

  /// PUT /api/v1/users/me/profile. 응답 본문은 쓰지 않는다 (상태는 로그인 상태로 반영).
  /// 실패는 AppException으로 던진다 (만료면 인터셉터가 먼저 갱신을 시도한다).
  Future<void> submit(Map<String, Object?> body) async {
    try {
      await _dio.put<void>('/api/v1/users/me/profile', data: body);
    } on DioException catch (e) {
      throw toAppException(e);
    }
  }
}

final profileApiProvider = Provider<ProfileApi>(
  (ref) => ProfileApi(ref.watch(authDioProvider)),
);
