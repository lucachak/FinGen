import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:fingen_mobile/features/auth/presentation/screens/login_screen.dart';
import 'package:fingen_mobile/features/dashboard/presentation/screens/dashboard_screen.dart';
import 'package:fingen_mobile/features/transactions/presentation/screens/transaction_list_screen.dart';
import 'package:fingen_mobile/features/transactions/presentation/screens/create_transaction_screen.dart';
import 'package:fingen_mobile/features/ai_assistant/presentation/screens/ai_chat_screen.dart';
import 'package:fingen_mobile/features/ai_assistant/presentation/screens/investment_plan_screen.dart';
import 'package:fingen_mobile/shared/theme/fingen_theme.dart';

import 'package:lucide_icons_flutter/lucide_icons.dart';
import 'package:google_fonts/google_fonts.dart';

final goRouter = GoRouter(
  initialLocation: '/login',
  routes: [
    GoRoute(
      path: '/login',
      builder: (context, state) => const LoginScreen(),
    ),
    ShellRoute(
      builder: (context, state, child) => MainShell(child: child),
      routes: [
        GoRoute(
          path: '/',
          builder: (context, state) => const DashboardScreen(),
        ),
        GoRoute(
          path: '/transactions',
          builder: (context, state) => const TransactionListScreen(),
        ),
        GoRoute(
          path: '/ai',
          builder: (context, state) => const AiChatScreen(),
        ),
        GoRoute(
          path: '/ai-plan',
          builder: (context, state) => const InvestmentPlanScreen(),
        ),
        GoRoute(
          path: '/create-transaction',
          builder: (context, state) => const CreateTransactionScreen(),
        ),
      ],
    ),
  ],
);

class MainShell extends StatelessWidget {
  final Widget child;
  const MainShell({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    final location = GoRouterState.of(context).matchedLocation;
    final colors = FinGenTheme.colors(context);

    return Scaffold(
      body: child,
      bottomNavigationBar: Container(
        decoration: BoxDecoration(
          color: Theme.of(context).scaffoldBackgroundColor,
          border: Border(
            top: BorderSide(color: colors.border.withOpacity(0.5), width: 0.5),
          ),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.05),
              blurRadius: 10,
              offset: const Offset(0, -2),
            ),
          ],
        ),
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                _buildNavItem(
                  context,
                  icon: LucideIcons.layoutDashboard,
                  label: 'Home',
                  isSelected: location == '/',
                  onTap: () => context.go('/'),
                ),
                _buildNavItem(
                  context,
                  icon: LucideIcons.arrowRightLeft,
                  label: 'Contas',
                  isSelected: location.startsWith('/transactions'),
                  onTap: () => context.go('/transactions'),
                ),
                _buildNavItem(
                  context,
                  icon: LucideIcons.brain,
                  label: 'Cérebro',
                  isSelected: location.startsWith('/ai'),
                  onTap: () => context.go('/ai'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildNavItem(
    BuildContext context, {
    required IconData icon,
    required String label,
    required bool isSelected,
    required VoidCallback onTap,
  }) {
    final colors = FinGenTheme.colors(context);
    final color = isSelected ? FinGenTheme.brand : colors.text3;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        decoration: BoxDecoration(
          color: isSelected ? FinGenTheme.brand.withOpacity(0.08) : Colors.transparent,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: color, size: 22),
            const SizedBox(height: 4),
            Text(
              label,
              style: GoogleFonts.dmSans(
                color: color,
                fontSize: 10,
                fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
                letterSpacing: 0.5,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
