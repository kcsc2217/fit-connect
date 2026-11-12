import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/network/api_exception.dart';
import 'package:trainer_ssaem/core/theme/app_colors.dart';
import 'package:trainer_ssaem/core/theme/app_theme.dart';
import 'package:trainer_ssaem/core/widgets/app_card.dart';
import 'package:trainer_ssaem/core/widgets/primary_button.dart';
import 'package:trainer_ssaem/features/auth/domain/auth_controller.dart';
import 'package:trainer_ssaem/features/me/data/me_api.dart';

/// "나" 탭: 내 정보와 로그아웃
class MeScreen extends ConsumerWidget {
  const MeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final text = Theme.of(context).textTheme;
    final me = ref.watch(meProvider);

    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 24, 20, 24),
        children: [
          Text('내 정보', style: text.headlineSmall),
          const SizedBox(height: 20),
          switch (me) {
            AsyncData(:final value) => _Profile(me: value),
            // 세션 만료면 라우터가 로그인 화면으로 보내므로 따로 표시하지 않는다
            AsyncError(error: SessionExpiredException()) => const SizedBox(),
            AsyncError() => _LoadError(
              onRetry: () => ref.invalidate(meProvider),
            ),
            _ => const Padding(
              padding: EdgeInsets.all(40),
              child: Center(child: CircularProgressIndicator()),
            ),
          },
          const SizedBox(height: 28),
          PrimaryButton.outlined(
            label: '로그아웃',
            leading: const Icon(Icons.logout_rounded, size: 18),
            onPressed: () =>
                ref.read(authControllerProvider.notifier).signOut(),
          ),
        ],
      ),
    );
  }
}

class _Profile extends StatelessWidget {
  const _Profile({required this.me});

  final Me me;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              CircleAvatar(
                radius: 28,
                backgroundColor: AppColors.lime.withValues(alpha: 0.14),
                child: Text(
                  me.nickname.characters.first,
                  style: text.titleMedium?.copyWith(color: AppColors.lime),
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Text(
                  me.nickname,
                  style: text.titleMedium?.copyWith(fontSize: 20),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          _InfoRow(
            label: '가입일',
            value: DateFormat('yyyy.MM.dd').format(me.createdAt.toLocal()),
          ),
          const SizedBox(height: 10),
          _InfoRow(
            label: '로그인 방법',
            value: me.providers.map((p) => p.label).join(', '),
          ),
        ],
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: BoxDecoration(
        color: AppColors.surfaceHigh,
        borderRadius: BorderRadius.circular(AppRadius.tile),
      ),
      child: Row(
        children: [
          Text(
            label,
            style: text.bodyMedium?.copyWith(color: AppColors.textSecondary),
          ),
          const Spacer(),
          Text(
            value,
            style: text.bodyMedium?.copyWith(fontWeight: FontWeight.w600),
          ),
        ],
      ),
    );
  }
}

class _LoadError extends StatelessWidget {
  const _LoadError({required this.onRetry});

  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Column(
        children: [
          Text(
            '내 정보를 불러오지 못했어요.',
            style: Theme.of(context).textTheme.bodyLarge,
          ),
          const SizedBox(height: 12),
          TextButton(onPressed: onRetry, child: const Text('다시 시도')),
        ],
      ),
    );
  }
}
