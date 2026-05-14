import 'package:json_annotation/json_annotation.dart';

part 'dashboard_models.g.dart';

@JsonSerializable()
class DashboardSummary {
  @JsonKey(fromJson: _anyToDouble)
  final double? gastosCasa;
  @JsonKey(fromJson: _anyToDouble)
  final double? gastosPessoal;
  @JsonKey(fromJson: _anyToDouble)
  final double? gastosNegocio;
  @JsonKey(fromJson: _anyToDouble)
  final double? freeCashFlow;
  @JsonKey(fromJson: _anyToDouble)
  final double? totalReceitas;
  @JsonKey(fromJson: _anyToDouble)
  final double? totalDespesas;
  @JsonKey(fromJson: _anyToDouble)
  final double? patrimonioLiquido;
  @JsonKey(fromJson: _anyToDouble)
  final double? totalDespesasPendentes;

  DashboardSummary({
    this.gastosCasa,
    this.gastosPessoal,
    this.gastosNegocio,
    this.freeCashFlow,
    this.totalReceitas,
    this.totalDespesas,
    this.patrimonioLiquido,
    this.totalDespesasPendentes,
  });

  factory DashboardSummary.fromJson(Map<String, dynamic> json) => _$DashboardSummaryFromJson(json);
  Map<String, dynamic> toJson() => _$DashboardSummaryToJson(this);
}

double? _anyToDouble(dynamic value) {
  if (value == null) return null;
  if (value is double) return value;
  if (value is int) return value.toDouble();
  if (value is String) return double.tryParse(value);
  return null;
}

@JsonSerializable()
class KpiModel {
  final String title;
  final String value;
  final double change; // Percentage change
  final bool isPositive;

  KpiModel({
    required this.title,
    required this.value,
    required this.change,
    required this.isPositive,
  });

  factory KpiModel.fromJson(Map<String, dynamic> json) => _$KpiModelFromJson(json);
  Map<String, dynamic> toJson() => _$KpiModelToJson(this);
}
