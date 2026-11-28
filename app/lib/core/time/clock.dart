import 'package:flutter_riverpod/flutter_riverpod.dart';

/// 지금 시각. 테스트에서 연말 경계 같은 시각을 고정하려고 provider로 둔다 (서버의 Clock 빈과 같은 역할).
final clockProvider = Provider<DateTime Function()>((ref) => DateTime.now);
