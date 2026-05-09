package lucas.basemodel.modules.financeiro.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lucas.basemodel.modules.financeiro.enums.*;
import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.repositories.CategoriaRepository;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@lombok.extern.slf4j.Slf4j
public class GeminiService {

    private final ContaRepository contaRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository; // 1. O Novo Injetor
    private final RestTemplate restTemplate;

    @Value("${python.microservice.url:http://127.0.0.1:8000}")
    private String pythonBaseUrl;

    public boolean isServiceAvailable() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(pythonBaseUrl + "/", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }


    public GeminiService(ContaRepository contaRepository, CategoriaRepository categoriaRepository, UsuarioRepository usuarioRepository) {
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;

        // Security Fix: Timeouts to prevent thread starvation if Python API hangs
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);   // 5s to establish connection
        factory.setReadTimeout(60000);     // 60s to wait for AI response
        this.restTemplate = new RestTemplate(factory);
    }

    // --- 1. MÉTODO DE INSIGHTS (O ASSISTENTE DE IA) ---
    public String gerarInsightsFinanceiros(String usernameResponsavel) {
        User responsavelReal = usuarioRepository.findByEmail(usernameResponsavel).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        List<Conta> contas = contaRepository.findByResponsavelAndEscopo(responsavelReal, EscopoTransacao.PESSOAL);
        List<Map<String, Object>> contasSimplificadas = new ArrayList<>();

        for (Conta c : contas) {
            Map<String, Object> contaMap = new HashMap<>();
            contaMap.put("valor", c.getValor());
            contaMap.put("tipo", c.getTipo().name());
            contaMap.put("categoria", c.getCategoria() != null ? c.getCategoria().getNome() : "Sem Categoria");
            contaMap.put("responsavel", c.getResponsavel() != null ? c.getResponsavel().getNomeCompleto() : "Desconhecido");
            contaMap.put("descricao", c.getDescricao());
            contaMap.put("data", c.getDataVencimento() != null ? c.getDataVencimento().toString() : "");
            contasSimplificadas.add(contaMap);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("contas", contasSimplificadas);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(pythonBaseUrl + "/api/ia/analisar", request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("insight")) {
                return (String) responseBody.get("insight");
            }
            return "<i>Análise não disponível no momento.</i>";
        } catch (Exception e) {
            return "<div style='color: #e11d48;'>🚨 O Córtex Analítico (Python) está offline. Inicie o servidor Python.</div>";
        }
    }

    // --- 2. IMPORTAÇÃO DE EXTRATO (COM PROTEÇÃO ENTERPRISE) ---

    // Removed @Transactional to prevent DB connection pool exhaustion on 30-60s AI calls
    public Map<String, Object> processarExtratoIA(MultipartFile file, String usernameResponsavel) {
        Map<String, Object> resultado = new HashMap<>();
        try {
            User responsavelReal = usuarioRepository.findByEmail(usernameResponsavel).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));

            Map<String, String> historicoUsuario = new HashMap<>();
            for (Conta c : contaRepository.findByResponsavelAndEscopo(responsavelReal, EscopoTransacao.PESSOAL)) {
                if (c.getCategoria() != null) {
                    historicoUsuario.put(c.getDescricao().toUpperCase(), c.getCategoria().getNome().toUpperCase().replace(" ", "_"));
                }
            }
            String historicoJson = new ObjectMapper().writeValueAsString(historicoUsuario);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", file.getResource());
            body.add("historico", historicoJson);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(pythonBaseUrl + "/api/ia/extrato", requestEntity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && "sucesso".equals(responseBody.get("status"))) {
                String titularPdf = (String) responseBody.get("titular");
                User responsavelFinal = responsavelReal;

                String titularNormalizado = titularPdf != null ? titularPdf.trim().replaceAll("\\s+", " ") : "";

                if (!titularNormalizado.isEmpty() && !titularNormalizado.equalsIgnoreCase("Desconhecido")) {
                    Optional<User> userEncontrado = usuarioRepository.findByNomeCompletoIgnoreCase(titularNormalizado);
                    if (userEncontrado.isPresent()) {
                        responsavelFinal = userEncontrado.get();
                    }
                }

                List<Map<String, Object>> transacoes = (List<Map<String, Object>>) responseBody.get("transacoes");
                Map<String, Categoria> categoriaMap = new HashMap<>();
                for (Categoria cat : categoriaRepository.findAll()) {
                    categoriaMap.put(cat.getNome().toLowerCase(), cat);
                }
                
                List<Conta> loteDeContas = new ArrayList<>();
                BigDecimal totalEntradas = BigDecimal.ZERO;
                BigDecimal totalSaidas = BigDecimal.ZERO;
                Map<String, BigDecimal> gastosPorCategoria = new HashMap<>();

                if (transacoes != null) {
                    for (Map<String, Object> t : transacoes) {
                        try {
                            Conta novaConta = new Conta();
                            novaConta.setDescricao((String) t.get("descricao"));
                            BigDecimal valorTransacao = new BigDecimal(t.get("valor").toString()).abs();
                            novaConta.setValor(valorTransacao);
                            String tipoStr = (String) t.get("tipo");
                            TipoTransacao tipoT;
                            if ("ENTRADA".equalsIgnoreCase(tipoStr) || "RECEITA".equalsIgnoreCase(tipoStr)) {
                                tipoT = TipoTransacao.RECEITA;
                            } else {
                                tipoT = TipoTransacao.DESPESA;
                            }
                            novaConta.setTipo(tipoT);

                            LocalDate data = LocalDate.parse((String) t.get("data"));
                            novaConta.setDataVencimento(data);
                            novaConta.setDataPagamento(data);
                            novaConta.setPaga(true);
                            
                            String freqObj = (String) t.get("frequencia");
                            if (freqObj != null) {
                                try { novaConta.setFrequencia(Frequencia.valueOf(freqObj)); } catch(Exception ignored) {}
                            }

                            String catIaBruta = (String) t.get("categoria");
                            String[] palavras = catIaBruta.split("_");
                            StringBuilder formatado = new StringBuilder();
                            for (String p : palavras) {
                                formatado.append(p.substring(0, 1).toUpperCase()).append(p.substring(1).toLowerCase()).append(" ");
                            }
                            String nomeCatFormatado = formatado.toString().trim();

                            Categoria categoriaMapeada = categoriaMap.get(nomeCatFormatado.toLowerCase());
                            if (categoriaMapeada == null) {
                                categoriaMapeada = new Categoria();
                                categoriaMapeada.setNome(nomeCatFormatado);
                                categoriaMapeada.setNatureza(lucas.basemodel.modules.financeiro.enums.NaturezaCategoria.DESPESA);
                                categoriaMapeada = categoriaRepository.save(categoriaMapeada);
                                categoriaMap.put(nomeCatFormatado.toLowerCase(), categoriaMapeada);
                            }

                            novaConta.setCategoria(categoriaMapeada);
                            novaConta.setPrioridade(Prioridade.MEDIA);
                            novaConta.setResponsavel(responsavelFinal);
                            novaConta.setEscopo(EscopoTransacao.PESSOAL);

                            // DO NOT SAVE TO DB YET. THIS IS JUST STAGING.
                            loteDeContas.add(novaConta);

                            if (tipoT == TipoTransacao.RECEITA) {
                                totalEntradas = totalEntradas.add(valorTransacao);
                            } else {
                                totalSaidas = totalSaidas.add(valorTransacao);
                                gastosPorCategoria.merge(categoriaMapeada.getNome(), valorTransacao, BigDecimal::add);
                            }
                        } catch (Exception ex) {
                            log.error("Erro ao mapear transação: ", ex);
                        }
                    }
                }

                resultado.put("status", "sucesso");
                resultado.put("loteDeContas", loteDeContas);
                resultado.put("titularPdf", titularPdf != null ? titularPdf : "Desconhecido");
                resultado.put("responsavelReal", responsavelReal);
                resultado.put("responsavelFinal", responsavelFinal);
                resultado.put("totalEntradas", totalEntradas);
                resultado.put("totalSaidas", totalSaidas);
                resultado.put("gastosPorCategoria", gastosPorCategoria);
                
                Map<String, Object> debug = (Map<String, Object>) responseBody.get("_debug");
                if (debug != null) {
                    resultado.put("via_ia", debug.get("categorizadas_via_ia"));
                }
                
                return resultado;

            } else {
                resultado.put("status", "erro");
                resultado.put("mensagem", responseBody != null ? responseBody.get("mensagem") : "Falha na leitura");
                return resultado;
            }
        } catch (Exception e) {
            log.error("Erro interno no processarExtratoIA: ", e);
            resultado.put("status", "erro");
            resultado.put("mensagem", "Erro interno ao comunicar com o servidor de Inteligência Artificial.");
            return resultado;
        }
    }



    // --- A LENTE: PROCESSAR FOTO DE RECIBO ---
    public String processarReciboIA(MultipartFile file, String usernameResponsavel) {
        try {
            User responsavelReal = usuarioRepository.findByEmail(usernameResponsavel).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", file.getResource());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(pythonBaseUrl + "/api/ia/recibo", request, Map.class);
            Map<String, Object> bodyResp = response.getBody();

            if (bodyResp != null && "sucesso".equals(bodyResp.get("status"))) {
                Map<String, Object> t = (Map<String, Object>) bodyResp.get("transacao");

                Conta novaConta = new Conta();
                novaConta.setDescricao((String) t.get("descricao"));
                novaConta.setValor(new BigDecimal(t.get("valor").toString()).abs());
                String tipoStrRec = (String) t.get("tipo");
                if ("ENTRADA".equalsIgnoreCase(tipoStrRec) || "RECEITA".equalsIgnoreCase(tipoStrRec)) {
                    novaConta.setTipo(TipoTransacao.RECEITA);
                } else {
                    novaConta.setTipo(TipoTransacao.DESPESA);
                }

                try {
                    novaConta.setDataVencimento(LocalDate.parse((String) t.get("data")));
                } catch (Exception e) {
                    novaConta.setDataVencimento(LocalDate.now());
                }
                novaConta.setDataPagamento(novaConta.getDataVencimento());
                novaConta.setPaga(true);
                
                Object recObj = t.get("frequencia");
                if (recObj != null && recObj instanceof String) {
                    try { novaConta.setFrequencia(Frequencia.valueOf((String) recObj)); } catch(Exception ignored) {}
                }

                List<Categoria> cats = categoriaRepository.findAll();
                String catBruta = (String) t.get("categoria");
                Categoria cat = cats.stream().filter(c -> c.getNome().toUpperCase().contains(catBruta))
                        .findFirst().orElse(!cats.isEmpty() ? cats.get(0) : null);

                novaConta.setCategoria(cat);
                novaConta.setPrioridade(Prioridade.BAIXA);
                novaConta.setResponsavel(responsavelReal);

                contaRepository.save(novaConta);

                return "<div style='color: #10b981; font-weight: bold;'>📸 Mágica! " + novaConta.getDescricao() + " guardado.</div>" +
                        "<script>setTimeout(function(){ window.location.reload(); }, 1500);</script>";
            }
            return "<div style='color: #ef4444;'>A IA não conseguiu ler o talão.</div>";
        } catch (Exception e) {
            return "<div style='color: #ef4444;'>Erro de comunicação visual com o Python.</div>";
        }
    }
    // --- CONSULTOR DE INVESTIMENTOS (WEALTH MANAGEMENT) ---
    public String gerarPlanoInvestimentos(String usernameResponsavel) {
        try {
            User responsavelReal = usuarioRepository.findByEmail(usernameResponsavel).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));

            // Puxa APENAS as contas privadas do utilizador logado
            List<Conta> contasPessoais = contaRepository.findByResponsavelAndEscopo(responsavelReal, EscopoTransacao.PESSOAL);
            List<Map<String, Object>> contasSimplificadas = new ArrayList<>();

            for (Conta c : contasPessoais) {
                // Filtramos apenas contas pagas/efetivadas ou deste mês para o cálculo real
                if (c.isPaga()) {
                    Map<String, Object> contaMap = new HashMap<>();
                    contaMap.put("valor", c.getValor());
                    contaMap.put("tipo", c.getTipo().name());
                    contasSimplificadas.add(contaMap);
                }
            }

            Map<String, Object> perfilMap = new HashMap<>();
            perfilMap.put("tipo", responsavelReal.getTipoPerfilFinanceiro() != null ? responsavelReal.getTipoPerfilFinanceiro() : "CONSERVADOR");
            perfilMap.put("meta_poupanca", responsavelReal.getMetaPoupancaMensal() != null ? responsavelReal.getMetaPoupancaMensal() : new BigDecimal("20.00"));
            perfilMap.put("teto_essenciais", responsavelReal.getTetoGastosEssenciais() != null ? responsavelReal.getTetoGastosEssenciais() : new BigDecimal("50.00"));
            perfilMap.put("orcamento_mensal", responsavelReal.getOrcamentoMensal() != null ? responsavelReal.getOrcamentoMensal() : new BigDecimal("3500.00"));

            Map<String, Object> body = new HashMap<>();
            body.put("transacoes", contasSimplificadas);
            body.put("perfil", perfilMap);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(pythonBaseUrl + "/api/ia/investimentos", request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("relatorio")) {
                return (String) responseBody.get("relatorio");
            }
            return "<i>Análise não disponível.</i>";
        } catch (Exception e) {
            log.error("Erro no gerarPlanoInvestimentos: ", e);
            return "<div style='color: #ef4444;'>🚨 O servidor Python está offline ou inacessível.</div>";
        }
    }
    // --- 1. DETETIVE FINANCEIRO (ANÁLISE DE ANOMALIAS) ---
    public String analisarAnomalias(String usernameResponsavel) {
        try {
            // Identifica quem está a pedir a análise
            User responsavelReal = usuarioRepository.findByEmail(usernameResponsavel).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));

            // Scalability Fix: use scoped queries instead of findAll()
            List<Conta> contasCasa = contaRepository.findByResponsavelAndEscopo(responsavelReal, EscopoTransacao.CASA);
            List<Conta> contasPessoais = contaRepository.findByResponsavelAndEscopo(responsavelReal, EscopoTransacao.PESSOAL);
            List<Conta> todasContas = new ArrayList<>(contasCasa);
            todasContas.addAll(contasPessoais);
            
            // Limit to last 6 months to avoid overflowing context window
            LocalDate limite = LocalDate.now().minusMonths(6);
            List<Conta> contasRecentes = todasContas.stream()
                .filter(c -> c.getDataVencimento() != null && c.getDataVencimento().isAfter(limite))
                .toList();
                
            List<Map<String, Object>> contasSimplificadas = new ArrayList<>();

            for (Conta c : contasRecentes) {
                    Map<String, Object> contaMap = new HashMap<>();
                    contaMap.put("valor", c.getValor());
                    contaMap.put("tipo", c.getTipo().name());
                    contaMap.put("categoria", c.getCategoria() != null ? c.getCategoria().getNome() : "Sem Categoria");
                    contaMap.put("responsavel", c.getResponsavel() != null ? c.getResponsavel().getNomeCompleto() : "Desconhecido");
                    contaMap.put("descricao", c.getDescricao());
                    contaMap.put("data", c.getDataVencimento() != null ? c.getDataVencimento().toString() : LocalDate.now().toString());
                    contasSimplificadas.add(contaMap);
            }

            Map<String, Object> perfilMap = new HashMap<>();
            perfilMap.put("tipo", responsavelReal.getTipoPerfilFinanceiro() != null ? responsavelReal.getTipoPerfilFinanceiro() : "CONSERVADOR");
            perfilMap.put("meta_poupanca", responsavelReal.getMetaPoupancaMensal() != null ? responsavelReal.getMetaPoupancaMensal() : new BigDecimal("20.00"));
            perfilMap.put("teto_essenciais", responsavelReal.getTetoGastosEssenciais() != null ? responsavelReal.getTetoGastosEssenciais() : new BigDecimal("50.00"));
            perfilMap.put("orcamento_mensal", responsavelReal.getOrcamentoMensal() != null ? responsavelReal.getOrcamentoMensal() : new BigDecimal("3500.00"));

            Map<String, Object> body = new HashMap<>();
            body.put("contas", contasSimplificadas);
            body.put("perfil", perfilMap);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(pythonBaseUrl + "/api/ia/analisar", request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("insight")) {
                return (String) responseBody.get("insight");
            }
            return "<i>Análise não disponível no momento.</i>";

        } catch (Exception e) {
            log.error("Erro no analisarAnomalias: ", e);
            return "<div style='color: #e11d48; padding: 15px; background: #fee2e2; border-radius: 8px; border: 1px solid #fca5a5;'>🚨 O Detetive IA (Python) está offline ou inacessível.</div>";
        }
    }

    // --- 5. PREVISOR DE CONTAS VARIÁVEIS (AUTOMAÇÃO INTELIGENTE) ---
    public BigDecimal preverVariacao(List<Conta> historico) {
        if (historico == null || historico.isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            List<Map<String, Object>> historicoSimples = new ArrayList<>();
            for (Conta c : historico) {
                Map<String, Object> map = new HashMap<>();
                map.put("valor", c.getValor());
                map.put("data", c.getDataVencimento() != null ? c.getDataVencimento().toString() : "");
                historicoSimples.add(map);
            }
            body.put("historico", historicoSimples);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(pythonBaseUrl + "/api/ia/prever", request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("valorCalculado")) {
                return new BigDecimal(responseBody.get("valorCalculado").toString());
            }
        } catch (Exception e) {
            log.error("Erro de conexão ao prever com Gemini IA. Acionando Fallback Matemático: ", e);
        }

        // Fallback: Média aritmética simples
        BigDecimal soma = BigDecimal.ZERO;
        for (Conta c : historico) {
            soma = soma.add(c.getValor());
        }
        return soma.divide(new BigDecimal(historico.size()), java.math.RoundingMode.HALF_UP);
    }
}