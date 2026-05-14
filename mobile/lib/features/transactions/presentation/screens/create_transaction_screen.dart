import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import 'package:fingen_mobile/shared/theme/fingen_theme.dart';
import 'package:fingen_mobile/shared/widgets/zen_card.dart';
import 'package:fingen_mobile/features/transactions/data/transaction_repository.dart';
import 'package:fingen_mobile/features/transactions/data/models/transaction_models.dart';

class CreateTransactionScreen extends ConsumerStatefulWidget {
  const CreateTransactionScreen({super.key});

  @override
  ConsumerState<CreateTransactionScreen> createState() => _CreateTransactionScreenState();
}

class _CreateTransactionScreenState extends ConsumerState<CreateTransactionScreen> {
  final _formKey = GlobalKey<FormState>();
  final _descricaoController = TextEditingController();
  final _valorController = TextEditingController();
  TransactionType _tipo = TransactionType.expense;
  TransactionScope _escopo = TransactionScope.personal;
  TransactionPriority _prioridade = TransactionPriority.medium;
  TransactionFrequency _frequencia = TransactionFrequency.oneTime;
  DateTime _dataVencimento = DateTime.now();

  @override
  void dispose() {
    _descricaoController.dispose();
    _valorController.dispose();
    super.dispose();
  }

  void _onSave() async {
    if (!_formKey.currentState!.validate()) return;

    final tx = TransactionModel(
      descricao: _descricaoController.text,
      valor: double.parse(_valorController.text),
      tipo: _tipo,
      escopo: _escopo,
      prioridade: _prioridade,
      frequencia: _frequencia,
      dataVencimento: _dataVencimento.toIso8601String().split('T')[0],
      categoriaId: 1, // Placeholder
      responsavelId: 'user-id', // Placeholder
    );

    try {
      await ref.read(transactionRepositoryProvider).createTransaction(tx);
      if (mounted) context.pop();
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Erro ao salvar: $e', style: GoogleFonts.dmSans()),
          backgroundColor: FinGenTheme.error,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = FinGenTheme.colors(context);

    return Scaffold(
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      appBar: AppBar(
        title: Text(
          'Nova Transação',
          style: GoogleFonts.notoSerifJp(
            fontWeight: FontWeight.bold,
            color: colors.text1,
          ),
        ),
        centerTitle: true,
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          children: [
            ZenCard(
              padding: const EdgeInsets.all(24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'DETALHES BÁSICOS',
                    style: GoogleFonts.dmSans(
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                      color: colors.text3,
                      letterSpacing: 1.5,
                    ),
                  ),
                  const SizedBox(height: 20),
                  _buildTextField(_descricaoController, 'Descrição do lançamento', LucideIcons.fileText),
                  const SizedBox(height: 16),
                  _buildTextField(
                    _valorController, 
                    'Valor (R\$)', 
                    LucideIcons.dollarSign, 
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
            ZenCard(
              padding: const EdgeInsets.all(24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'CLASSIFICAÇÃO',
                    style: GoogleFonts.dmSans(
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                      color: colors.text3,
                      letterSpacing: 1.5,
                    ),
                  ),
                  const SizedBox(height: 20),
                  _buildDropdown<TransactionType>(
                    'Natureza do Fluxo',
                    _tipo,
                    TransactionType.values,
                    (val) => setState(() => _tipo = val!),
                    LucideIcons.arrowRightLeft,
                  ),
                  const SizedBox(height: 16),
                  _buildDropdown<TransactionScope>(
                    'Escopo Financeiro',
                    _escopo,
                    TransactionScope.values,
                    (val) => setState(() => _escopo = val!),
                    LucideIcons.layers,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 32),
            ElevatedButton(
              onPressed: _onSave,
              style: ElevatedButton.styleFrom(
                backgroundColor: FinGenTheme.brand,
                foregroundColor: Colors.white,
                minimumSize: const Size(double.infinity, 60),
                elevation: 4,
                shadowColor: FinGenTheme.brand.withOpacity(0.4),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              ),
              child: Text(
                'SALVAR TRANSAÇÃO',
                style: GoogleFonts.dmSans(
                  fontWeight: FontWeight.bold,
                  letterSpacing: 1.2,
                ),
              ),
            ),
            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }

  Widget _buildTextField(TextEditingController controller, String label, IconData icon, {TextInputType? keyboardType}) {
    final colors = FinGenTheme.colors(context);
    return TextFormField(
      controller: controller,
      keyboardType: keyboardType,
      style: GoogleFonts.dmSans(color: colors.text1, fontWeight: FontWeight.w500),
      validator: (v) => v!.isEmpty ? 'Campo obrigatório' : null,
      decoration: InputDecoration(
        labelText: label,
        labelStyle: GoogleFonts.dmSans(color: colors.text3, fontSize: 13),
        prefixIcon: Icon(icon, color: colors.text3, size: 18),
        filled: true,
        fillColor: colors.surface2,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: colors.border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: colors.border),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: FinGenTheme.brand, width: 1.5),
        ),
      ),
    );
  }

  Widget _buildDropdown<T extends Enum>(String label, T value, List<T> items, ValueChanged<T?> onChanged, IconData icon) {
    final colors = FinGenTheme.colors(context);
    return DropdownButtonFormField<T>(
      value: value,
      items: items.map((e) => DropdownMenuItem(
        value: e, 
        child: Text(
          e.name.toUpperCase(),
          style: GoogleFonts.dmSans(fontSize: 13, fontWeight: FontWeight.w500),
        )
      )).toList(),
      onChanged: onChanged,
      style: GoogleFonts.dmSans(color: colors.text1),
      decoration: InputDecoration(
        labelText: label,
        labelStyle: GoogleFonts.dmSans(color: colors.text3, fontSize: 13),
        prefixIcon: Icon(icon, color: colors.text3, size: 18),
        filled: true,
        fillColor: colors.surface2,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: colors.border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: colors.border),
        ),
      ),
    );
  }
}
