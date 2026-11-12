/// 서버 AuthProvider enum과 같은 값. [wire]가 서버로 오가는 문자열이다.
enum AuthProvider {
  kakao('KAKAO', '카카오'),
  google('GOOGLE', 'Google'),
  apple('APPLE', 'Apple'),
  dev('DEV', '개발용');

  const AuthProvider(this.wire, this.label);

  final String wire;

  /// 화면에 보이는 이름
  final String label;

  /// 모르는 값(서버에 새 provider가 생긴 경우)은 null.
  static AuthProvider? fromWire(String value) {
    for (final p in values) {
      if (p.wire == value) return p;
    }
    return null;
  }
}

/// 서버 UserStatus. 앱은 ONBOARDING·ACTIVE만 구분한다.
enum UserStatus {
  onboarding,
  active;

  /// ONBOARDING이 아니면 모두 active로 본다 (화면 분기에 필요한 구분만 한다).
  static UserStatus fromWire(String value) =>
      value == 'ONBOARDING' ? UserStatus.onboarding : UserStatus.active;
}

/// 서버가 발급한 토큰 묶음 (docs/decisions/0004).
class AuthTokens {
  const AuthTokens({
    required this.accessToken,
    required this.accessTokenExpiresAt,
    required this.refreshToken,
    required this.refreshTokenExpiresAt,
  });

  factory AuthTokens.fromJson(Map<String, dynamic> json) => AuthTokens(
    accessToken: json['accessToken'] as String,
    accessTokenExpiresAt: DateTime.parse(
      json['accessTokenExpiresAt'] as String,
    ),
    refreshToken: json['refreshToken'] as String,
    refreshTokenExpiresAt: DateTime.parse(
      json['refreshTokenExpiresAt'] as String,
    ),
  );

  final String accessToken;
  final DateTime accessTokenExpiresAt;
  final String refreshToken;
  final DateTime refreshTokenExpiresAt;

  @override
  bool operator ==(Object other) =>
      other is AuthTokens &&
      other.accessToken == accessToken &&
      other.accessTokenExpiresAt == accessTokenExpiresAt &&
      other.refreshToken == refreshToken &&
      other.refreshTokenExpiresAt == refreshTokenExpiresAt;

  @override
  int get hashCode => Object.hash(
    accessToken,
    accessTokenExpiresAt,
    refreshToken,
    refreshTokenExpiresAt,
  );
}
