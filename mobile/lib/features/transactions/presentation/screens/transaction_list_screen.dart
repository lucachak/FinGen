import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import 'package:fingen_mobile/shared/theme/fingen_theme.dart';
import 'package:fingen_mobile/shared/widgets/zen_card.dart';
import 'package:fingen_mobile/features/transactions/data/transaction_repository.dart';
import 'package:fingen_mobile/features/transactions/data/models/transaction_models.dart';
import 'package:intl/intl.dart';

class TransactionListScreen extends ConsumerWidget {
  const TransactionListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final transactionsAsync = ref.watch(transactionsListProvider());
    final colors = FinGenTheme.colors(context);

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
        title: Text(
          'Transações',
          style: GoogleFonts.notoSerifJp(
            fontWeight: FontWeight.bold,
            color: colors.text1,
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(LucideIcons.filter, size: 20),
            onPressed: () {},
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: transactionsAsync.when(
        data: (transactions) => _buildList(context, transactions, ref),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, stack) => Center(child: Text('Erro: $err')),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => context.push('/create-transaction'),
        backgroundColor: FinGenTheme.brand,
        elevation: 4,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        child: const Icon(LucideIcons.plus, color: Colors.white),
      ),
    );
  }

  Widget _buildList(BuildContext context, List<TransactionModel> transactions, WidgetRef ref) {
    final colors = FinGenTheme.colors(context);
    
    if (transactions.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(LucideIcons.receipt, size: 48, color: colors.text3.withOpacity(0.3)),
            const SizedBox(height: 16),
            Text(
              'Nenhuma transação encontrada',
              style: GoogleFonts.dmSans(color: colors.text3, fontWeight: FontWeight.w500),
            ),
          ],
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      itemCount: transactions.length,
      itemBuilder: (context, index) {
        final tx = transactions[index];
        return _buildTransactionCard(context, tx);
      },
    );
  }

  Widget _buildTransactionCard(BuildContext context, TransactionModel tx) {
    final colors = FinGenTheme.colors(context);
    final isIncome = tx.tipo == TransactionType.income;
    final color = isIncome ? colors.success : colors.text1;
    final icon = isIncome ? LucideIcons.arrowUpRight : LucideIcons.arrowDownLeft;
    final iconColor = isIncome ? colors.success : colors.text3;

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: ZenCard(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: isIncome ? colors.success.withOpacity(0.1) : colors.surface2,
                shape: BoxShape.circle,
              ),
              child: Icon(icon, color: iconColor, size: 18),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    tx.descricao,
                    style: GoogleFonts.dmSans(
                      color: colors.text1,
                      fontWeight: FontWeight.bold,
                      fontSize: 14,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 2),
                  Text(
                    tx.dataVencimento,
                    style: GoogleFonts.dmSans(color: colors.text3, fontSize: 11),
                  ),
                ],
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  '${isIncome ? '+ ' : '- '}R\$ ${tx.valor.toStringAsFixed(2)}',
                  style: GoogleFonts.dmMono(
                    color: color,
                    fontWeight: FontWeight.w600,
                    fontSize: 14,
                  ),
                ),
                const SizedBox(height: 4),
                _buildStatusBadge(tx.status, colors),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatusBadge(TransactionStatus status, FinGenColors colors) {
    Color bg;
    Color text;
    String label;

    switch (status) {
      case TransactionStatus.paid:
        bg = colors.success.withOpacity(0.1);
        text = colors.success;
        label = 'PAGO';
        break;
      case TransactionStatus.pending:
        bg = Colors.amber.withOpacity(0.1);
        text = Colors.amber.shade800;
        label = 'PENDENTE';
        break;
      case TransactionStatus.overdue:
        bg = colors.error.withOpacity(0.1);
        text = colors.error;
        label = 'ATRASADO';
        break;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: text.withOpacity(0.2)),
      ),
      child: Text(
        label,
        style: GoogleFonts.dmSans(
          color: text,
          fontSize: 8,
          fontWeight: FontWeight.bold,
          letterSpacing: 0.5,
        ),
      ),
    );
  }
}
