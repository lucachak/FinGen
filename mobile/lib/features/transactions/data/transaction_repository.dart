import 'dart:io';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../shared/api/api_client.dart';
import '../../../shared/api/api_provider.dart';
import 'models/transaction_models.dart';

part 'transaction_repository.g.dart';

class TransactionRepository {
  final ApiClient _apiClient;

  TransactionRepository(this._apiClient);

  Future<List<TransactionModel>> getTransactions({
    TransactionStatus? status,
    TransactionScope? escopo,
    int? page,
  }) async {
    return await _apiClient.getTransactions(
      status: status?.name.toUpperCase(),
      escopo: escopo?.name.toUpperCase(),
      page: page,
    );
  }

  Future<TransactionModel> createTransaction(TransactionModel transaction, {File? file}) async {
    return await _apiClient.createTransaction(
      descricao: transaction.descricao,
      valor: transaction.valor,
      tipo: transaction.tipo.name.toUpperCase(),
      escopo: transaction.escopo.name.toUpperCase(),
      prioridade: transaction.prioridade.name.toUpperCase(),
      frequencia: transaction.frequencia.name.toUpperCase(),
      dataVencimento: transaction.dataVencimento,
      categoriaId: transaction.categoriaId,
      responsavelId: transaction.responsavelId,
      file: file,
    );
  }

  Future<void> markAsPaid(String id) async {
    await _apiClient.markAsPaid(id);
  }

  Future<void> deleteTransaction(String id) async {
    await _apiClient.deleteTransaction(id);
  }
}

@riverpod
TransactionRepository transactionRepository(TransactionRepositoryRef ref) {
  final client = ref.watch(apiClientProvider);
  return TransactionRepository(client);
}

@riverpod
Future<List<TransactionModel>> transactionsList(
  TransactionsListRef ref, {
  TransactionStatus? status,
  TransactionScope? escopo,
}) async {
  final repository = ref.watch(transactionRepositoryProvider);
  return await repository.getTransactions(status: status, escopo: escopo);
}
