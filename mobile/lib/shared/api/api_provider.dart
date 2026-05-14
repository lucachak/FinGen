import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import 'api_client.dart';
import 'auth_interceptor.dart';

part 'api_provider.g.dart';

@riverpod
FlutterSecureStorage secureStorage(SecureStorageRef ref) {
  return const FlutterSecureStorage();
}

@riverpod
Dio dio(DioRef ref) {
  final dio = Dio();
  final storage = ref.watch(secureStorageProvider);
  dio.interceptors.add(AuthInterceptor(storage));
  return dio;
}

@riverpod
ApiClient apiClient(ApiClientRef ref) {
  final dioClient = ref.watch(dioProvider);
  // Para testar no celular físico, usamos o seu IP da rede local
  // Para emulador, seria http://10.0.2.2:8080/api/v1
  const baseUrl = "http://192.168.18.209:8080/api/v1/"; 
  return ApiClient(dioClient, baseUrl: baseUrl);
}
