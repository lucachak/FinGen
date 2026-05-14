import 'package:riverpod_annotation/riverpod_annotation.dart';
import '../../../shared/api/api_client.dart';
import '../../../shared/api/api_provider.dart';

part 'ai_repository.g.dart';

class AiRepository {
  final ApiClient _apiClient;

  AiRepository(this._apiClient);

  Future<String> getChatResponse(String message) async {
    try {
      // O backend espera uma LISTA de mensagens (padrão OpenAI/OpenRouter)
      final messages = [
        {"role": "user", "content": message}
      ];
      final response = await _apiClient.chat(messages);
      
      if (response is String) return response;
      if (response is Map) {
        return response['content'] ?? response['reply'] ?? response['response'] ?? '';
      }
      return '';
    } catch (e) {
      rethrow;
    }
  }

  Future<String> getInvestmentPlan() async {
    try {
      final response = await _apiClient.getPersonalConsultant();
      if (response is String) return response;
      if (response is Map) {
        return response['plan'] ?? response['content'] ?? response['reply'] ?? '';
      }
      return response?.toString() ?? '';
    } catch (e) {
      rethrow;
    }
  }
}

@riverpod
AiRepository aiRepository(AiRepositoryRef ref) {
  final client = ref.watch(apiClientProvider);
  return AiRepository(client);
}
