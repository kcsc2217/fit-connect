import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_timezone/flutter_timezone.dart';

/// 기기 시간대를 IANA 이름(예: Asia/Seoul)으로 읽는다.
/// Dart의 DateTime.timeZoneName은 "KST" 같은 약어라 서버 ZoneId가 해석하지 못한다.
final timeZoneReaderProvider = Provider<Future<String> Function()>(
  (ref) =>
      () async => (await FlutterTimezone.getLocalTimezone()).identifier,
);

/// 읽지 못하거나, 응답이 없거나, 서버가 모르는 값이면 Asia/Seoul (온보딩을 막지 않는다. 첫 사용자는 한국)
final deviceTimeZoneProvider = FutureProvider<String>((ref) async {
  try {
    final name =
        (await ref
                .read(timeZoneReaderProvider)()
                .timeout(ref.read(timeZoneReadTimeoutProvider)))
            .trim();
    // 서버 ZoneId가 해석하지 못하는 값(빈 값, "KST" 같은 약어)은 보내지 않는다
    return _ianaName.hasMatch(name) ? name : fallbackTimeZone;
  } on Object {
    return fallbackTimeZone; // 실패·타임아웃
  }
});

// "Asia/Seoul", "America/Argentina/Buenos_Aires", "Etc/GMT+9", "UTC"
final _ianaName = RegExp(r'^(UTC|[A-Za-z]+(/[A-Za-z0-9_+\-]+)+)$');

const fallbackTimeZone = 'Asia/Seoul';

/// 시간대 읽기를 기다리는 최대 시간 (플랫폼 채널이 응답하지 않아도 제출이 멈추지 않게)
final timeZoneReadTimeoutProvider = Provider<Duration>(
  (ref) => const Duration(seconds: 2),
);
