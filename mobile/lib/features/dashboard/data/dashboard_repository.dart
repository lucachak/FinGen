import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../shared/api/api_client.dart';
import '../../../shared/api/api_provider.dart';
import 'models/dashboard_models.dart';

part 'dashboard_repository.g.dart';

class DashboardRepository {
  final ApiClient _apiClient;

  DashboardRepository(this._apiClient);

  Future<DashboardSummary> getSummary() async {
    return await _apiClient.getSummary();
  }
}

@riverpod
DashboardRepository dashboardRepository(DashboardRepositoryRef ref) {
  final client = ref.watch(apiClientProvider);
  return DashboardRepository(client);
}

@riverpod
Future<DashboardSummary> dashboardSummary(DashboardSummaryRef ref) async {
  final repository = ref.watch(dashboardRepositoryProvider);
  return await repository.getSummary();
}
