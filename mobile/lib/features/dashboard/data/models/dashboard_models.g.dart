// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'dashboard_models.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DashboardSummary _$DashboardSummaryFromJson(Map<String, dynamic> json) =>
    DashboardSummary(
      gastosCasa: _anyToDouble(json['gastosCasa']),
      gastosPessoal: _anyToDouble(json['gastosPessoal']),
      gastosNegocio: _anyToDouble(json['gastosNegocio']),
      freeCashFlow: _anyToDouble(json['freeCashFlow']),
      totalReceitas: _anyToDouble(json['totalReceitas']),
      totalDespesas: _anyToDouble(json['totalDespesas']),
      patrimonioLiquido: _anyToDouble(json['patrimonioLiquido']),
      totalDespesasPendentes: _anyToDouble(json['totalDespesasPendentes']),
    );

Map<String, dynamic> _$DashboardSummaryToJson(DashboardSummary instance) =>
    <String, dynamic>{
      'gastosCasa': instance.gastosCasa,
      'gastosPessoal': instance.gastosPessoal,
      'gastosNegocio': instance.gastosNegocio,
      'freeCashFlow': instance.freeCashFlow,
      'totalReceitas': instance.totalReceitas,
      'totalDespesas': instance.totalDespesas,
      'patrimonioLiquido': instance.patrimonioLiquido,
      'totalDespesasPendentes': instance.totalDespesasPendentes,
    };

KpiModel _$KpiModelFromJson(Map<String, dynamic> json) => KpiModel(
      title: json['title'] as String,
      value: json['value'] as String,
      change: (json['change'] as num).toDouble(),
      isPositive: json['isPositive'] as bool,
    );

Map<String, dynamic> _$KpiModelToJson(KpiModel instance) => <String, dynamic>{
      'title': instance.title,
      'value': instance.value,
      'change': instance.change,
      'isPositive': instance.isPositive,
    };
