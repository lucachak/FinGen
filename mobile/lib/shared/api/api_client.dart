import 'dart:io';
import 'package:dio/dio.dart';
import 'package:retrofit/retrofit.dart';
import '../../features/auth/data/models/auth_models.dart';
import '../../features/transactions/data/models/transaction_models.dart';
import '../../features/dashboard/data/models/dashboard_models.dart';

part 'api_client.g.dart';

@RestApi()
abstract class ApiClient {
  factory ApiClient(Dio dio, {String baseUrl}) = _ApiClient;

  // --- Auth ---
  @POST("auth/register")
  Future<AuthResponse> register(@Body() RegisterRequest request);

  @POST("auth/login")
  Future<AuthResponse> login(@Body() LoginRequest request);

  @GET("auth/me")
  Future<AuthResponse> getCurrentUser();

  // --- Dashboard ---
  @GET("dashboard/summary")
  Future<DashboardSummary> getSummary();

  // --- Transactions ---
  @GET("contas")
  Future<List<TransactionModel>> getTransactions({
    @Query("status") String? status,
    @Query("escopo") String? escopo,
    @Query("page") int? page,
  });

  @POST("contas")
  @MultiPart()
  Future<TransactionModel> createTransaction({
    @Part() required String descricao,
    @Part() required double valor,
    @Part() required String tipo,
    @Part() required String escopo,
    @Part() required String prioridade,
    @Part() required String frequencia,
    @Part() required String dataVencimento,
    @Part() required int categoriaId,
    @Part() required String responsavelId,
    @Part(name: "comprovante") File? file,
  });

  @PATCH("contas/{id}/pagar")
  Future<void> markAsPaid(@Path("id") String id);

  @DELETE("contas/{id}")
  Future<void> deleteTransaction(@Path("id") String id);

  // --- AI ---
  @POST("ia/chat")
  Future<dynamic> chat(@Body() dynamic messages);

  @GET("ia/consultor-pessoal")
  Future<dynamic> getPersonalConsultant();

  @GET("ia/analisar-anomalias")
  Future<dynamic> analyzeAnomalies();
}
