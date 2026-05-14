import 'package:riverpod_annotation/riverpod_annotation.dart';
import 'package:fingen_mobile/shared/api/api_provider.dart';
import 'package:fingen_mobile/features/auth/data/auth_repository.dart';
import 'package:fingen_mobile/features/auth/data/models/auth_models.dart';

part 'auth_provider.g.dart';

class AuthState {
  final bool isLoggedIn;
  final AuthResponse? user;

  AuthState({required this.isLoggedIn, this.user});
}

@Riverpod(keepAlive: true)
class Auth extends _$Auth {
  @override
  Future<AuthState> build() async {
    final repository = ref.read(authRepositoryProvider);
    final isLoggedIn = await repository.isLoggedIn();
    if (isLoggedIn) {
      try {
        final user = await repository.getCurrentUser();
        return AuthState(isLoggedIn: true, user: user);
      } catch (_) {
        return AuthState(isLoggedIn: true);
      }
    }
    return AuthState(isLoggedIn: false);
  }

  Future<bool> login(String email, String password) async {
    state = const AsyncValue.loading();
    try {
      final repository = ref.read(authRepositoryProvider);
      final response = await repository.login(email, password);
      
      if (response.token != null && response.token!.isNotEmpty) {
        state = AsyncValue.data(AuthState(isLoggedIn: true, user: response));
        return true;
      }
      
      state = AsyncValue.error('Credenciais inválidas ou resposta incompleta', StackTrace.current);
      return false;
    } catch (e, stack) {
      state = AsyncValue.error(e, stack);
      return false;
    }
  }

  Future<void> logout() async {
    final repository = ref.read(authRepositoryProvider);
    await repository.logout();
    state = AsyncValue.data(AuthState(isLoggedIn: false));
  }
}
