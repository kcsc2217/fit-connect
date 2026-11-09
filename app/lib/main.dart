import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

import 'api/api_exception.dart';
import 'api/auth_api.dart';
import 'api/models.dart';
import 'auth/auth_repository.dart';
import 'auth/social_login.dart';
import 'auth/token_store.dart';
import 'config.dart';
import 'util/jwt_payload.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initSocialSdks();
  final repository = AuthRepository(AuthApi(http.Client(), AppConfig.apiBaseUrl), const SecureTokenStore());
  runApp(LoginCheckApp(repository: repository));
}

class LoginCheckApp extends StatelessWidget {
  const LoginCheckApp({super.key, required this.repository});

  final AuthRepository repository;

  @override
  Widget build(BuildContext context) => MaterialApp(
        title: '트레이너 쌤 로그인 확인',
        theme: ThemeData(colorSchemeSeed: Colors.teal, useMaterial3: true),
        home: LoginCheckPage(repository: repository),
      );
}

/// 로그인 확인용 한 화면: 로그인 → 내 정보 표시, 방금 받은 id_token의 iss·aud 표시
class LoginCheckPage extends StatefulWidget {
  const LoginCheckPage({super.key, required this.repository});

  final AuthRepository repository;

  @override
  State<LoginCheckPage> createState() => _LoginCheckPageState();
}

class _LoginCheckPageState extends State<LoginCheckPage> {
  final _devIdController = TextEditingController(text: 'dev-tester');

  bool _busy = false;
  Me? _me;
  String? _error;
  String? _lastProvider;
  IdTokenClaims? _lastClaims;

  AuthRepository get _repo => widget.repository;

  @override
  void initState() {
    super.initState();
    // 저장된 토큰이 있으면 바로 내 정보를 불러온다 (보안 저장소에 남아 있는지 확인용)
    _run(() async {
      try {
        return await _repo.fetchMe();
      } on NotLoggedInException {
        return null;
      }
    });
  }

  @override
  void dispose() {
    _devIdController.dispose();
    super.dispose();
  }

  /// 버튼 공통 처리: 진행 표시, 결과·에러 반영
  Future<void> _run(Future<Me?> Function() action) async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final me = await action();
      setState(() => _me = me);
    } catch (e) {
      setState(() => _error = _describe(e));
    } finally {
      setState(() => _busy = false);
    }
  }

  Future<void> _socialLogin(String provider, Future<String> Function() getIdToken) => _run(() async {
        final idToken = await getIdToken();
        setState(() {
          _lastProvider = provider;
          _lastClaims = IdTokenClaims.tryParse(idToken);
        });
        return _repo.loginAndFetchMe(provider, idToken);
      });

  /// 서버 에러는 code로 나눈다 (detail 문구로 분기하지 않는다)
  String _describe(Object e) {
    if (e is ApiException) {
      final message = switch (e.code) {
        'INVALID_SOCIAL_TOKEN' => '서버가 id_token을 거부했습니다 (aud·iss·서명·만료 확인)',
        'SOCIAL_LOGIN_UNAVAILABLE' => '소셜 로그인에 잠시 연결할 수 없습니다. 잠시 후 다시 시도하세요',
        'UNSUPPORTED_PROVIDER' => '서버에 이 로그인의 허용 aud가 설정되지 않았습니다',
        'INVALID_REFRESH_TOKEN' => '다시 로그인해 주세요',
        'UNAUTHORIZED' || 'TOKEN_EXPIRED' => '인증이 필요합니다. 다시 로그인해 주세요',
        _ => e.detail ?? '서버 오류',
      };
      return '$message\n(HTTP ${e.status}, code=${e.code})';
    }
    if (e is SocialLoginException) return e.message;
    return '$e';
  }

  @override
  Widget build(BuildContext context) {
    final me = _me;
    return Scaffold(
      appBar: AppBar(title: const Text('로그인 확인')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('서버: ${AppConfig.apiBaseUrl}', style: Theme.of(context).textTheme.bodySmall),
          const SizedBox(height: 16),
          Row(children: [
            Expanded(
              child: TextField(
                controller: _devIdController,
                decoration: const InputDecoration(labelText: '개발용 사용자 ID', border: OutlineInputBorder()),
              ),
            ),
            const SizedBox(width: 8),
            FilledButton(
              onPressed: _busy ? null : () => _run(() => _repo.loginAndFetchMe('DEV', _devIdController.text.trim())),
              child: const Text('DEV 로그인'),
            ),
          ]),
          const SizedBox(height: 12),
          FilledButton.tonal(
            onPressed: _busy || !AppConfig.kakaoEnabled ? null : () => _socialLogin('KAKAO', kakaoIdToken),
            child: Text(AppConfig.kakaoEnabled ? '카카오로 로그인' : '카카오 (keys.json에 키 없음)'),
          ),
          const SizedBox(height: 8),
          FilledButton.tonal(
            onPressed: _busy || !AppConfig.googleEnabled ? null : () => _socialLogin('GOOGLE', googleIdToken),
            child: Text(AppConfig.googleEnabled ? '구글로 로그인' : '구글 (keys.json에 키 없음)'),
          ),
          const SizedBox(height: 16),
          if (_busy) const LinearProgressIndicator(),
          if (_error != null)
            Card(
              color: Theme.of(context).colorScheme.errorContainer,
              child: Padding(padding: const EdgeInsets.all(12), child: SelectableText(_error!)),
            ),
          if (me != null)
            Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: SelectableText(
                  '내 정보 (GET /users/me)\n'
                  'id: ${me.id}\n'
                  '닉네임: ${me.nickname}\n'
                  'status: ${me.status}\n'
                  'providers: ${me.providers.join(', ')}\n'
                  '가입: ${me.createdAt.toLocal()}',
                ),
              ),
            ),
          if (_lastClaims != null)
            Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                // 서버 허용 목록(KAKAO_APP_KEYS / GOOGLE_CLIENT_IDS)에 넣을 값 확인용
                child: SelectableText(
                  '마지막 $_lastProvider id_token\n'
                  'iss: ${_lastClaims!.iss}\n'
                  'aud: ${_lastClaims!.aud.join(', ')}',
                ),
              ),
            ),
          const SizedBox(height: 8),
          Row(children: [
            OutlinedButton(
              onPressed: _busy ? null : () => _run(_repo.fetchMe),
              child: const Text('내 정보 다시 조회'),
            ),
            const SizedBox(width: 8),
            OutlinedButton(
              onPressed: _busy
                  ? null
                  : () => _run(() async {
                        await _repo.logout();
                        return null;
                      }),
              child: const Text('로그아웃'),
            ),
          ]),
        ],
      ),
    );
  }
}
