import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import 'package:fingen_mobile/shared/theme/fingen_theme.dart';
import 'package:fingen_mobile/shared/widgets/zen_card.dart';
import 'package:fingen_mobile/shared/widgets/hero_metric_card.dart';
import 'package:go_router/go_router.dart';
import 'package:fingen_mobile/features/auth/presentation/providers/auth_provider.dart';
import 'package:fingen_mobile/features/dashboard/data/dashboard_repository.dart';
import 'package:fingen_mobile/features/dashboard/data/models/dashboard_models.dart';
import 'package:intl/intl.dart';

class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authProvider);
    final user = authState.value?.user;
    final summaryAsync = ref.watch(dashboardSummaryProvider);
    final colors = FinGenTheme.colors(context);

    ref.listen(authProvider, (prev, next) {
      if (next.value?.isLoggedIn == false) {
        context.go('/login');
      }
    });

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
        title: Text(
          'FinGen',
          style: GoogleFonts.notoSerifJp(
            fontWeight: FontWeight.bold,
            color: colors.text1,
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(LucideIcons.sparkles, size: 20, color: FinGenTheme.brand),
            onPressed: () => context.push('/ai-plan'),
          ),
          IconButton(
            icon: const Icon(LucideIcons.bell, size: 20),
            onPressed: () {},
          ),
          const SizedBox(width: 8),
          GestureDetector(
            onTap: () {
              ref.read(authProvider.notifier).logout();
              context.go('/login');
            },
            child: CircleAvatar(
              radius: 16,
              backgroundColor: FinGenTheme.brand.withOpacity(0.1),
              child: const Icon(LucideIcons.user, size: 18, color: FinGenTheme.brand),
            ),
          ),
          const SizedBox(width: 16),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () => ref.refresh(dashboardSummaryProvider.future),
        child: summaryAsync.when(
          data: (summary) => _buildContent(context, summary, user?.username ?? 'Usuário'),
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (err, stack) => Center(child: Text('Erro: $err')),
        ),
      ),
    );
  }

  Widget _buildContent(BuildContext context, DashboardSummary summary, String userName) {
    final colors = FinGenTheme.colors(context);
    final now = DateTime.now();
    final monthName = DateFormat('MMMM yyyy', 'pt_BR').format(now);

    return ListView(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      children: [
        // 1. Header (Greeting)
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text(
                  'PANORAMA MENSAL • ',
                  style: GoogleFonts.dmMono(
                    color: FinGenTheme.brand,
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 1.5,
                  ),
                ),
                Text(
                  monthName.toUpperCase(),
                  style: GoogleFonts.dmMono(
                    color: colors.text3,
                    fontSize: 10,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            RichText(
              text: TextSpan(
                children: [
                  TextSpan(
                    text: 'Olá, ',
                    style: GoogleFonts.notoSerifJp(
                      fontSize: 32,
                      color: colors.text1,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  TextSpan(
                    text: '$userName.',
                    style: GoogleFonts.notoSerifJp(
                      fontSize: 32,
                      color: colors.text1,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ),
            Text(
              'Hoje é um bom dia para cultivar sua liberdade.',
              style: GoogleFonts.notoSerifJp(
                fontSize: 18,
                color: colors.text3,
                fontStyle: FontStyle.italic,
              ),
            ),
          ],
        ),
        const SizedBox(height: 32),

        // 2. Hero Metrics (Horizontal Scroll or Column)
        // Using a column of 2 rows for Bento look on mobile
        Row(
          children: [
            Expanded(
              child: HeroMetricCard(
                label: 'Patrimônio Líquido',
                value: NumberFormat.compact(locale: 'pt_BR').format(summary.patrimonioLiquido ?? 0),
                subtitle: '+4.2% total',
                icon: LucideIcons.gem,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: HeroMetricCard(
                label: 'Ganhos Mês',
                value: NumberFormat.compact(locale: 'pt_BR').format(summary.totalReceitas ?? 0),
                subtitle: 'Faturamento Real',
                icon: LucideIcons.arrowUpRight,
                gradient: const [Colors.white, Colors.white],
                textColor: colors.text1,
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: HeroMetricCard(
                label: 'Saídas Mês',
                value: NumberFormat.compact(locale: 'pt_BR').format(summary.totalDespesas ?? 0),
                subtitle: 'Contas pagas',
                icon: LucideIcons.arrowDownRight,
                gradient: const [Colors.white, Colors.white],
                textColor: colors.text1,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: HeroMetricCard(
                label: 'Fluxo Livre',
                value: NumberFormat.compact(locale: 'pt_BR').format(summary.freeCashFlow ?? 0),
                subtitle: 'Capacidade Invest.',
                icon: LucideIcons.droplets,
                gradient: const [Colors.white, Colors.white],
                textColor: FinGenTheme.brand,
              ),
            ),
          ],
        ),
        const SizedBox(height: 32),

        // 3. Quick Actions
        _buildSectionHeader('AÇÕES RÁPIDAS'),
        const SizedBox(height: 16),
        GridView.count(
          crossAxisCount: 2,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          mainAxisSpacing: 12,
          crossAxisSpacing: 12,
          childAspectRatio: 2.2,
          children: [
            _buildActionCard(LucideIcons.circlePlus, 'Nova Receita', 'Entrada de fundos', colors.success),
            _buildActionCard(LucideIcons.circleMinus, 'Lançar Saída', 'Despesa ou conta', colors.error),
            _buildActionCard(LucideIcons.car, 'Novo Ativo', 'Veículo ou imóvel', FinGenTheme.brand),
            _buildActionCard(LucideIcons.chartLine, 'Investimento', 'Mercado financeiro', Colors.lightBlue),
          ],
        ),
        const SizedBox(height: 32),

        // 4. Wealth Evolution Chart
        ZenCard(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Evolução do Patrimônio',
                    style: GoogleFonts.notoSerifJp(
                      fontSize: 18,
                      fontWeight: FontWeight.w600,
                      color: colors.text1,
                    ),
                  ),
                  Row(
                    children: [
                      Container(width: 8, height: 8, decoration: const BoxDecoration(color: FinGenTheme.brand, shape: BoxShape.circle)),
                      const SizedBox(width: 6),
                      Text('REAL', style: GoogleFonts.dmSans(fontSize: 10, fontWeight: FontWeight.bold, color: colors.text3)),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: 32),
              SizedBox(
                height: 200,
                child: LineChart(
                  LineChartData(
                    gridData: FlGridData(
                      show: true,
                      drawVerticalLine: false,
                      getDrawingHorizontalLine: (value) => FlLine(color: colors.border, strokeWidth: 1),
                    ),
                    titlesData: const FlTitlesData(show: false),
                    borderData: FlBorderData(show: false),
                    lineBarsData: [
                      LineChartBarData(
                        spots: [
                          const FlSpot(0, 3),
                          const FlSpot(1, 3.5),
                          const FlSpot(2, 3.2),
                          const FlSpot(3, 4.5),
                          const FlSpot(4, 4.8),
                          const FlSpot(5, 5.2),
                        ],
                        isCurved: true,
                        color: FinGenTheme.brand,
                        barWidth: 3,
                        isStrokeCapRound: true,
                        dotData: const FlDotData(show: false),
                        belowBarData: BarAreaData(
                          show: true,
                          gradient: LinearGradient(
                            colors: [FinGenTheme.brand.withOpacity(0.1), FinGenTheme.brand.withOpacity(0)],
                            begin: Alignment.topCenter,
                            end: Alignment.bottomCenter,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 24),

        // 5. Smart Insight Card
        ZenCard(
          color: FinGenTheme.brand,
          padding: const EdgeInsets.all(24),
          border: BorderSide.none,
          shadowSize: ZenShadowSize.lg,
          child: Stack(
            children: [
              Positioned(
                right: -20,
                bottom: -20,
                child: Icon(LucideIcons.zap, size: 80, color: Colors.white.withOpacity(0.1)),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(LucideIcons.zap, size: 16, color: Colors.amber),
                      const SizedBox(width: 8),
                      Text(
                        'SMART INSIGHT',
                        style: GoogleFonts.dmSans(
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                          color: Colors.white.withOpacity(0.8),
                          letterSpacing: 1.2,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'Você economizou R\$ 450 a mais este mês do que o habitual.',
                    style: GoogleFonts.dmSans(
                      fontSize: 14,
                      color: Colors.white,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextButton(
                    onPressed: () {},
                    style: TextButton.styleFrom(
                      backgroundColor: Colors.white.withOpacity(0.1),
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                    child: Text(
                      'Ver Relatório IA',
                      style: GoogleFonts.dmSans(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 32),
      ],
    );
  }

  Widget _buildSectionHeader(String title) {
    return Text(
      title,
      style: GoogleFonts.dmSans(
        fontSize: 10,
        fontWeight: FontWeight.bold,
        color: FinGenTheme.lightText3,
        letterSpacing: 2,
      ),
    );
  }

  Widget _buildActionCard(IconData icon, String title, String subtitle, Color color) {
    return ZenCard(
      padding: const EdgeInsets.all(12),
      child: Row(
        children: [
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              color: color.withOpacity(0.1),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, size: 18, color: color),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  title,
                  style: GoogleFonts.dmSans(fontSize: 12, fontWeight: FontWeight.bold, color: FinGenTheme.lightText1),
                  overflow: TextOverflow.ellipsis,
                ),
                Text(
                  subtitle,
                  style: GoogleFonts.dmSans(fontSize: 9, color: FinGenTheme.lightText3),
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
