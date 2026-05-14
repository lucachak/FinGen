// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'transaction_models.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

TransactionModel _$TransactionModelFromJson(Map<String, dynamic> json) =>
    TransactionModel(
      id: json['id'] as String?,
      descricao: json['descricao'] as String,
      valor: _anyToDouble(json['valor']),
      tipo: $enumDecode(_$TransactionTypeEnumMap, json['tipo']),
      escopo: $enumDecode(_$TransactionScopeEnumMap, json['escopo']),
      prioridade: $enumDecode(_$TransactionPriorityEnumMap, json['prioridade']),
      frequencia:
          $enumDecode(_$TransactionFrequencyEnumMap, json['frequencia']),
      dataVencimento: json['dataVencimento'] as String,
      categoriaId: (json['categoriaId'] as num).toInt(),
      responsavelId: json['responsavelId'] as String,
      status: $enumDecodeNullable(_$TransactionStatusEnumMap, json['status']) ??
          TransactionStatus.pending,
      comprovanteUrl: json['comprovanteUrl'] as String?,
    );

Map<String, dynamic> _$TransactionModelToJson(TransactionModel instance) =>
    <String, dynamic>{
      'id': instance.id,
      'descricao': instance.descricao,
      'valor': instance.valor,
      'tipo': _$TransactionTypeEnumMap[instance.tipo]!,
      'escopo': _$TransactionScopeEnumMap[instance.escopo]!,
      'prioridade': _$TransactionPriorityEnumMap[instance.prioridade]!,
      'frequencia': _$TransactionFrequencyEnumMap[instance.frequencia]!,
      'dataVencimento': instance.dataVencimento,
      'categoriaId': instance.categoriaId,
      'responsavelId': instance.responsavelId,
      'status': _$TransactionStatusEnumMap[instance.status]!,
      'comprovanteUrl': instance.comprovanteUrl,
    };

const _$TransactionTypeEnumMap = {
  TransactionType.income: 'RECEITA',
  TransactionType.expense: 'DESPESA',
};

const _$TransactionScopeEnumMap = {
  TransactionScope.home: 'CASA',
  TransactionScope.personal: 'PESSOAL',
  TransactionScope.business: 'NEGOCIO',
};

const _$TransactionPriorityEnumMap = {
  TransactionPriority.high: 'ALTA',
  TransactionPriority.medium: 'MEDIA',
  TransactionPriority.low: 'BAIXA',
};

const _$TransactionFrequencyEnumMap = {
  TransactionFrequency.oneTime: 'AVULSA',
  TransactionFrequency.monthly: 'MENSAL',
  TransactionFrequency.weekly: 'SEMANAL',
  TransactionFrequency.yearly: 'ANUAL',
};

const _$TransactionStatusEnumMap = {
  TransactionStatus.pending: 'PENDENTE',
  TransactionStatus.paid: 'PAGO',
  TransactionStatus.overdue: 'ATRASADO',
};
