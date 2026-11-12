import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/router.dart';

class TrainerSsaemApp extends ConsumerWidget {
  const TrainerSsaemApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return MaterialApp.router(
      title: '트레이너쌤',
      routerConfig: ref.watch(routerProvider),
    );
  }
}
