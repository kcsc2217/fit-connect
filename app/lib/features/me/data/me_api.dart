import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:trainer_ssaem/features/auth/data/auth_dio.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';

/// GET /api/v1/users/me 응답
class Me {
  const Me({
    required this.id,
    required this.nickname,
    required this.status,
    required this.createdAt,
    required this.providers,
  });

  final int id;
  final String nickname;
  final UserStatus status;
  final DateTime createdAt;

  /// 연결된 소셜 종류. 앱이 모르는 값은 뺀다.
  final List<AuthProvider> providers;
}

class MeApi {
  MeApi(this._dio);

  final Dio _dio;

  /// 실패는 AppException으로 던진다 (만료면 인터셉터가 먼저 갱신을 시도한다).
  Future<Me> fetch() => throw UnimplementedError();

  // 스텁 단계에서 필드 미사용 경고를 피하기 위한 접근 (구현 시 제거)
  Dio get dio => _dio;
}

final meApiProvider = Provider<MeApi>(
  (ref) => MeApi(ref.watch(authDioProvider)),
);

/// "나" 탭이 보여주는 내 정보. 탭을 떠나면 버린다.
final meProvider = FutureProvider.autoDispose<Me>(
  (ref) => ref.watch(meApiProvider).fetch(),
);
