import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

/// 화면 경로와, 로그인 상태에 따른 이동 규칙.
final routerProvider = Provider<GoRouter>(
  (ref) => GoRouter(
    routes: [GoRoute(path: '/', builder: (context, state) => const SizedBox())],
  ),
);
