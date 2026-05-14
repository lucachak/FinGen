import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import 'package:fingen_mobile/shared/api/api_client.dart';
import 'package:fingen_mobile/shared/api/api_provider.dart';
import 'package:fingen_mobile/features/auth/data/models/auth_models.dart';

part 'auth_repository.g.dart';

class AuthRepository {
  final ApiClient _apiClient;
  final FlutterSecureStorage _storage;

  AuthRepository(this._apiClient, this._storage);

  Future<AuthResponse> login(String email, String password) async {
    final response = await _apiClient.login(LoginRequest(email: email, password: password));
    if (response.token != null) {
      await _storage.write(key: 'jwt_token', value: response.token!);
    }
    if (response.userId != null) {
      await _storage.write(key: 'user_id', value: response.userId!);
    }
    return response;
  }

  Future<AuthResponse> register(String email, String username, String password) async {
    final response = await _apiClient.register(
      RegisterRequest(email: email, username: username, password: password),
    );
    if (response.token != null) {
      await _storage.write(key: 'jwt_token', value: response.token!);
    }
    return response;
  }

  Future<AuthResponse> getCurrentUser() async {
    return await _apiClient.getCurrentUser();
  }

  Future<void> logout() async {
    await _storage.delete(key: 'jwt_token');
    await _storage.delete(key: 'user_id');
  }

  Future<String?> getToken() async => await _storage.read(key: 'jwt_token');
  
  Future<bool> isLoggedIn() async {
    final token = await getToken();
    return token != null;
  }
}

@riverpod
AuthRepository authRepository(AuthRepositoryRef ref) {
  final client = ref.watch(apiClientProvider);
  const storage = FlutterSecureStorage();
  return AuthRepository(client, storage);
}
