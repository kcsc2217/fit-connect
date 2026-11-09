/// POST /api/v1/auth/login 응답 (만료 시각은 이 앱에서 쓰지 않아 읽지 않는다)
class LoginResult {
  const LoginResult(this.accessToken, this.refreshToken, this.status);

  final String accessToken;
  final String refreshToken;
  final String status;

  factory LoginResult.fromJson(Map<String, dynamic> json) => LoginResult(
        json['accessToken'] as String,
        json['refreshToken'] as String,
        json['status'] as String,
      );
}

/// POST /api/v1/auth/refresh 응답. refresh는 쓸 때마다 새 값으로 바뀐다.
class TokenPair {
  const TokenPair(this.accessToken, this.refreshToken);

  final String accessToken;
  final String refreshToken;

  factory TokenPair.fromJson(Map<String, dynamic> json) =>
      TokenPair(json['accessToken'] as String, json['refreshToken'] as String);
}

/// GET /api/v1/users/me 응답
class Me {
  const Me(this.id, this.nickname, this.status, this.createdAt, this.providers);

  final int id;
  final String nickname;
  final String status;
  final DateTime createdAt;
  final List<String> providers;

  factory Me.fromJson(Map<String, dynamic> json) => Me(
        json['id'] as int,
        json['nickname'] as String,
        json['status'] as String,
        DateTime.parse(json['createdAt'] as String),
        (json['providers'] as List<dynamic>).cast<String>(),
      );
}
