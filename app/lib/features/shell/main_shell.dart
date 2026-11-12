import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';
import 'package:trainer_ssaem/core/widgets/coming_soon_view.dart';

/// 하단 탭 5개 (프로토타입: 오늘, 식단, 트레이너, 챌린지, 나)
class MainShell extends StatelessWidget {
  const MainShell({required this.navigationShell, super.key});

  final StatefulNavigationShell navigationShell;

  static const tabs = [
    (label: '오늘', icon: Icons.today_outlined, selected: Icons.today_rounded),
    (
      label: '식단',
      icon: Icons.restaurant_outlined,
      selected: Icons.restaurant_rounded,
    ),
    (
      label: '트레이너',
      icon: Icons.auto_awesome_outlined,
      selected: Icons.auto_awesome_rounded,
    ),
    (
      label: '챌린지',
      icon: Icons.emoji_events_outlined,
      selected: Icons.emoji_events_rounded,
    ),
    (
      label: '나',
      icon: Icons.person_outline_rounded,
      selected: Icons.person_rounded,
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: navigationShell,
      bottomNavigationBar: NavigationBar(
        selectedIndex: navigationShell.currentIndex,
        onDestinationSelected: (index) => navigationShell.goBranch(
          index,
          // 이미 선택된 탭을 다시 누르면 그 탭의 첫 화면으로
          initialLocation: index == navigationShell.currentIndex,
        ),
        destinations: [
          for (final tab in tabs)
            NavigationDestination(
              icon: Icon(tab.icon),
              selectedIcon: Icon(tab.selected),
              label: tab.label,
            ),
        ],
      ),
    );
  }
}

/// "오늘" 탭 (오늘의 운동 기능에서 채운다)
class TodayTab extends StatelessWidget {
  const TodayTab({super.key});

  @override
  Widget build(BuildContext context) => const ComingSoonView(
    title: '오늘의 운동',
    description: '알림 시각에 내 기록에 맞춘 오늘의 운동을 알려드릴게요.',
    icon: Icons.fitness_center_rounded,
  );
}

class DietTab extends StatelessWidget {
  const DietTab({super.key});

  @override
  Widget build(BuildContext context) => const ComingSoonView(
    title: '식단',
    description: '오늘 목표 칼로리 안에서 먹을 메뉴를 추천해 드릴게요.',
    icon: Icons.restaurant_rounded,
  );
}

class CoachTab extends StatelessWidget {
  const CoachTab({super.key});

  @override
  Widget build(BuildContext context) => const ComingSoonView(
    title: 'AI 트레이너',
    description: '내 기록을 아는 트레이너에게 몸 상태를 물어볼 수 있어요.',
    icon: Icons.auto_awesome_rounded,
  );
}

class ChallengeTab extends StatelessWidget {
  const ChallengeTab({super.key});

  @override
  Widget build(BuildContext context) => const ComingSoonView(
    title: '챌린지',
    description: '함께 기록하고 30일을 채우는 챌린지가 열릴 거예요.',
    icon: Icons.emoji_events_rounded,
  );
}
