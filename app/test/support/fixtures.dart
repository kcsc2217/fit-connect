import 'package:trainer_ssaem/features/auth/domain/auth_models.dart';

/// 테스트용 토큰 묶음
AuthTokens tokens(String access, String refresh) => AuthTokens(
  accessToken: access,
  accessTokenExpiresAt: DateTime.utc(2026, 11, 12, 12, 30),
  refreshToken: refresh,
  refreshTokenExpiresAt: DateTime.utc(2026, 11, 26, 12),
);

/// 서버 토큰 응답 본문 (갱신)
Map<String, Object?> tokenJson(String access, String refresh) => {
  'accessToken': access,
  'accessTokenExpiresAt': '2026-11-12T12:30:00Z',
  'refreshToken': refresh,
  'refreshTokenExpiresAt': '2026-11-26T12:00:00Z',
};

/// 서버 로그인 응답 본문
Map<String, Object?> loginJson(
  String access,
  String refresh, {
  String status = 'ACTIVE',
}) => {...tokenJson(access, refresh), 'status': status};

/// 서버 내 정보 응답 본문
Map<String, Object?> meJson({
  String nickname = '회원1234',
  String status = 'ACTIVE',
  String createdAt = '2026-11-05T03:00:00Z',
  List<String> providers = const ['KAKAO'],
}) => {
  'id': 1,
  'nickname': nickname,
  'status': status,
  'createdAt': createdAt,
  'providers': providers,
};

const mePath = '/api/v1/users/me';
const loginPath = '/api/v1/auth/login';
const refreshPath = '/api/v1/auth/refresh';
const logoutPath = '/api/v1/auth/logout';
const profilePath = '/api/v1/users/me/profile';
