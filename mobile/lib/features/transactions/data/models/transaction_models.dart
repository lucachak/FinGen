import 'package:json_annotation/json_annotation.dart';

part 'transaction_models.g.dart';

enum TransactionType {
  @JsonValue('RECEITA')
  income,
  @JsonValue('DESPESA')
  expense,
}

enum TransactionScope {
  @JsonValue('CASA')
  home,
  @JsonValue('PESSOAL')
  personal,
  @JsonValue('NEGOCIO')
  business,
}

enum TransactionPriority {
  @JsonValue('ALTA')
  high,
  @JsonValue('MEDIA')
  medium,
  @JsonValue('BAIXA')
  low,
}

enum TransactionFrequency {
  @JsonValue('AVULSA')
  oneTime,
  @JsonValue('MENSAL')
  monthly,
  @JsonValue('SEMANAL')
  weekly,
  @JsonValue('ANUAL')
  yearly,
}

enum TransactionStatus {
  @JsonValue('PENDENTE')
  pending,
  @JsonValue('PAGO')
  paid,
  @JsonValue('ATRASADO')
  overdue,
}

@JsonSerializable()
class TransactionModel {
  final String? id;
  final String descricao;
  @JsonKey(fromJson: _anyToDouble)
  final double valor;
  final TransactionType tipo;
  final TransactionScope escopo;
  final TransactionPriority prioridade;
  final TransactionFrequency frequencia;
  final String dataVencimento;
  final int categoriaId;
  final String responsavelId;
  final TransactionStatus status;
  final String? comprovanteUrl;

  TransactionModel({
    this.id,
    required this.descricao,
    required this.valor,
    required this.tipo,
    required this.escopo,
    required this.prioridade,
    required this.frequencia,
    required this.dataVencimento,
    required this.categoriaId,
    required this.responsavelId,
    this.status = TransactionStatus.pending,
    this.comprovanteUrl,
  });

  factory TransactionModel.fromJson(Map<String, dynamic> json) => _$TransactionModelFromJson(json);
  Map<String, dynamic> toJson() => _$TransactionModelToJson(this);
}

double _anyToDouble(dynamic value) {
  if (value is double) return value;
  if (value is int) return value.toDouble();
  if (value is String) return double.tryParse(value) ?? 0.0;
  return 0.0;
}
