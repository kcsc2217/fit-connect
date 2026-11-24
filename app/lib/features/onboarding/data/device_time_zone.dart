import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_timezone/flutter_timezone.dart';

/// 기기 시간대를 IANA 이름(예: Asia/Seoul)으로 읽는다.
/// Dart의 DateTime.timeZoneName은 "KST" 같은 약어라 서버 ZoneId가 해석하지 못한다.
final timeZoneReaderProvider = Provider<Future<String> Function()>(
  (ref) =>
      () async => (await FlutterTimezone.getLocalTimezone()).identifier,
);

/// 읽지 못하면 Asia/Seoul (온보딩을 막지 않는다. 첫 사용자는 한국)
final deviceTimeZoneProvider = FutureProvider<String>((ref) async {
  try {
    return await ref.read(timeZoneReaderProvider)();
  } on Object {
    return fallbackTimeZone;
  }
});

const fallbackTimeZone = 'Asia/Seoul';
