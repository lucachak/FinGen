import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import 'package:fingen_mobile/shared/theme/fingen_theme.dart';
import 'package:fingen_mobile/shared/widgets/zen_card.dart';
import 'package:fingen_mobile/features/ai_assistant/data/ai_repository.dart';

class InvestmentPlanScreen extends ConsumerWidget {
  const InvestmentPlanScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final planAsync = ref.watch(investmentPlanProvider);
    final colors = FinGenTheme.colors(context);

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
        title: Text(
          'Plano de Investimento',
          style: GoogleFonts.notoSerifJp(
            fontWeight: FontWeight.bold,
            color: colors.text1,
          ),
        ),
      ),
      body: planAsync.when(
        data: (plan) => _buildContent(context, plan),
        loading: () => const Center(child: CircularProgressIndicator(color: FinGenTheme.brand)),
        error: (err, stack) => Center(child: Text('Erro: $err')),
      ),
    );
  }

  Widget _buildContent(BuildContext context, String plan) {
    final colors = FinGenTheme.colors(context);
    
    return ListView(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
      children: [
        Row(
          children: [
            const Icon(LucideIcons.sparkles, color: FinGenTheme.brand, size: 20),
            const SizedBox(width: 12),
            Text(
              'ESTRATÉGIA IA',
              style: GoogleFonts.dmSans(
                fontSize: 12,
                fontWeight: FontWeight.bold,
                color: FinGenTheme.brand,
                letterSpacing: 2,
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Text(
          'Seu Plano Personalizado',
          style: GoogleFonts.notoSerifJp(
            color: colors.text1,
            fontSize: 28,
            fontWeight: FontWeight.w600,
            letterSpacing: -0.5,
          ),
        ),
        const SizedBox(height: 24),
        ZenCard(
          padding: const EdgeInsets.all(24),
          shadowSize: ZenShadowSize.md,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    width: 4,
                    height: 24,
                    decoration: BoxDecoration(
                      color: FinGenTheme.brand,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Text(
                    'RECOMENDAÇÕES',
                    style: GoogleFonts.dmSans(
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                      color: colors.text3,
                      letterSpacing: 1.5,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              Text(
                plan.isEmpty ? 'Nenhum plano gerado ainda. Converse com a IA para começar!' : plan,
                style: GoogleFonts.dmSans(
                  color: colors.text1,
                  fontSize: 15,
                  height: 1.6,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 32),
        OutlinedButton.icon(
          onPressed: () => Navigator.pop(context),
          icon: const Icon(LucideIcons.messageSquare, size: 18),
          label: const Text('AJUSTAR COM A IA'),
          style: OutlinedButton.styleFrom(
            minimumSize: const Size(double.infinity, 56),
          ),
        ),
      ],
    );
  }
}

final investmentPlanProvider = FutureProvider<String>((ref) async {
  return await ref.watch(aiRepositoryProvider).getInvestmentPlan();
});
