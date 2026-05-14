// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'transaction_repository.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$transactionRepositoryHash() =>
    r'74a0208caa8647abfb4692b0becaaf3ca67f7221';

/// See also [transactionRepository].
@ProviderFor(transactionRepository)
final transactionRepositoryProvider =
    AutoDisposeProvider<TransactionRepository>.internal(
  transactionRepository,
  name: r'transactionRepositoryProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$transactionRepositoryHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

typedef TransactionRepositoryRef
    = AutoDisposeProviderRef<TransactionRepository>;
String _$transactionsListHash() => r'59e15a484a993d280730c460da3eef1e09c503f1';

/// Copied from Dart SDK
class _SystemHash {
  _SystemHash._();

  static int combine(int hash, int value) {
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + value);
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + ((0x0007ffff & hash) << 10));
    return hash ^ (hash >> 6);
  }

  static int finish(int hash) {
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + ((0x03ffffff & hash) << 3));
    // ignore: parameter_assignments
    hash = hash ^ (hash >> 11);
    return 0x1fffffff & (hash + ((0x00003fff & hash) << 15));
  }
}

/// See also [transactionsList].
@ProviderFor(transactionsList)
const transactionsListProvider = TransactionsListFamily();

/// See also [transactionsList].
class TransactionsListFamily
    extends Family<AsyncValue<List<TransactionModel>>> {
  /// See also [transactionsList].
  const TransactionsListFamily();

  /// See also [transactionsList].
  TransactionsListProvider call({
    TransactionStatus? status,
    TransactionScope? escopo,
  }) {
    return TransactionsListProvider(
      status: status,
      escopo: escopo,
    );
  }

  @override
  TransactionsListProvider getProviderOverride(
    covariant TransactionsListProvider provider,
  ) {
    return call(
      status: provider.status,
      escopo: provider.escopo,
    );
  }

  static const Iterable<ProviderOrFamily>? _dependencies = null;

  @override
  Iterable<ProviderOrFamily>? get dependencies => _dependencies;

  static const Iterable<ProviderOrFamily>? _allTransitiveDependencies = null;

  @override
  Iterable<ProviderOrFamily>? get allTransitiveDependencies =>
      _allTransitiveDependencies;

  @override
  String? get name => r'transactionsListProvider';
}

/// See also [transactionsList].
class TransactionsListProvider
    extends AutoDisposeFutureProvider<List<TransactionModel>> {
  /// See also [transactionsList].
  TransactionsListProvider({
    TransactionStatus? status,
    TransactionScope? escopo,
  }) : this._internal(
          (ref) => transactionsList(
            ref as TransactionsListRef,
            status: status,
            escopo: escopo,
          ),
          from: transactionsListProvider,
          name: r'transactionsListProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product')
                  ? null
                  : _$transactionsListHash,
          dependencies: TransactionsListFamily._dependencies,
          allTransitiveDependencies:
              TransactionsListFamily._allTransitiveDependencies,
          status: status,
          escopo: escopo,
        );

  TransactionsListProvider._internal(
    super._createNotifier, {
    required super.name,
    required super.dependencies,
    required super.allTransitiveDependencies,
    required super.debugGetCreateSourceHash,
    required super.from,
    required this.status,
    required this.escopo,
  }) : super.internal();

  final TransactionStatus? status;
  final TransactionScope? escopo;

  @override
  Override overrideWith(
    FutureOr<List<TransactionModel>> Function(TransactionsListRef provider)
        create,
  ) {
    return ProviderOverride(
      origin: this,
      override: TransactionsListProvider._internal(
        (ref) => create(ref as TransactionsListRef),
        from: from,
        name: null,
        dependencies: null,
        allTransitiveDependencies: null,
        debugGetCreateSourceHash: null,
        status: status,
        escopo: escopo,
      ),
    );
  }

  @override
  AutoDisposeFutureProviderElement<List<TransactionModel>> createElement() {
    return _TransactionsListProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is TransactionsListProvider &&
        other.status == status &&
        other.escopo == escopo;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, status.hashCode);
    hash = _SystemHash.combine(hash, escopo.hashCode);

    return _SystemHash.finish(hash);
  }
}

mixin TransactionsListRef
    on AutoDisposeFutureProviderRef<List<TransactionModel>> {
  /// The parameter `status` of this provider.
  TransactionStatus? get status;

  /// The parameter `escopo` of this provider.
  TransactionScope? get escopo;
}

class _TransactionsListProviderElement
    extends AutoDisposeFutureProviderElement<List<TransactionModel>>
    with TransactionsListRef {
  _TransactionsListProviderElement(super.provider);

  @override
  TransactionStatus? get status => (origin as TransactionsListProvider).status;
  @override
  TransactionScope? get escopo => (origin as TransactionsListProvider).escopo;
}
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member
