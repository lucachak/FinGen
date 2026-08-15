package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.core.exceptions.ResourceNotFoundException;
import lucas.basemodel.core.exceptions.BadRequestException;
import lucas.basemodel.modules.financeiro.dto.ContaRequest;
import lucas.basemodel.modules.financeiro.dto.ContaResponse;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.StatusTransacao;
import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.repositories.CategoriaRepository;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lucas.basemodel.modules.wealth.models.Asset;
import lucas.basemodel.modules.wealth.models.BankAccountAsset;
import lucas.basemodel.modules.wealth.repositories.AssetRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class ContaService {

    private final ContaRepository contaRepository;
    private final AssetRepository assetRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final CategoriaService categoriaService;
    private final EspacoFinanceiroService espacoFinanceiroService;

    public ContaService(ContaRepository contaRepository, 
                        AssetRepository assetRepository,
                        UsuarioRepository usuarioRepository,
                        CategoriaRepository categoriaRepository,
                        CategoriaService categoriaService,
                        EspacoFinanceiroService espacoFinanceiroService) {
        this.contaRepository = contaRepository;
        this.assetRepository = assetRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.categoriaService = categoriaService;
        this.espacoFinanceiroService = espacoFinanceiroService;
    }

    /**
     * Entrada segura para os formulários MVC. O proprietário nunca é aceito do
     * cliente e uma edição só ocorre quando o registro já pertence ao usuário.
     */
    @Transactional
    public Conta salvarParaUsuario(Conta conta, User usuario) {
        Objects.requireNonNull(usuario, "Usuário autenticado é obrigatório");
        espacoFinanceiroService.validarAcesso(usuario, conta.getEscopo());

        if (conta.getId() != null) {
            contaRepository.findByIdAndResponsavelId(conta.getId(), usuario.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada"));
        }

        if (conta.getCategoria() != null && conta.getCategoria().getId() != null) {
            conta.setCategoria(categoriaService.validarNoEscopo(conta.getCategoria().getId(), conta.getEscopo()));
        }

        if (conta.getAsset() != null && conta.getAsset().getId() != null) {
            conta.setAsset(resolveAsset(conta.getAsset().getId(), usuario));
        }

        conta.setResponsavel(usuario);
        conta.setResponsaveisRateio(new ArrayList<>(List.of(usuario)));
        return salvar(conta);
    }

    @Transactional
    Conta salvar(Conta conta) {

        categoriaService.validarNoEscopo(conta.getCategoria(), conta.getEscopo());

        if (conta.getValor() == null) {
            conta.setValor(BigDecimal.ZERO);
        }

        // VALIDAÇÃO: Transação PAGA deve ter um Ativo (conta bancária) associado
        // para garantir que o Patrimônio Líquido seja atualizado corretamente.
        if (conta.isPaga() && conta.getAsset() == null) {
            throw new IllegalStateException(
                "Não é possível marcar uma transação como Paga sem vincular uma Conta Bancária ou Carteira. " +
                "Por favor, selecione um Ativo no formulário antes de salvar."
            );
        }

        // 1. MODO DE EDIÇÃO: Se a conta já tem ID, estamos apenas atualizando uma fração/conta existente
        if (conta.getId() != null) {
            Conta antiga = contaRepository.findById(conta.getId()).orElse(null);
            
            if (conta.isPaga() && conta.getDataPagamento() == null) {
                conta.setDataPagamento(LocalDate.now());
            } else if (!conta.isPaga()) {
                conta.setDataPagamento(null);
            }

            // Lógica de Sincronização na Edição: Se mudou para Paga agora
            if (antiga != null && !antiga.isPaga() && conta.isPaga() && conta.getAsset() != null) {
                atualizarSaldoAtivo(conta.getAsset(), conta.getValor(), conta.getTipo());
            }

            // Se editou o morador usando os novos checkboxes
            if (conta.getResponsaveisRateio() != null && !conta.getResponsaveisRateio().isEmpty()) {
                conta.setResponsavel(conta.getResponsaveisRateio().get(0));
            }
            return contaRepository.save(conta);
        }

        // 2. MODO DE CRIAÇÃO: Rateio e Recorrência
        int meses = (conta.getMesesRecorrencia() != null && conta.getMesesRecorrencia() > 0) ? conta.getMesesRecorrencia() : 1;

        List<User> responsaveis = conta.getResponsaveisRateio();
        if (responsaveis == null) responsaveis = new ArrayList<>();

        boolean isRateio = responsaveis.size() > 1;

        // Se marcou mais de uma pessoa, fatiamos o valor!
        BigDecimal valorBase = conta.getValor();
        if (isRateio && valorBase != null) {
            valorBase = valorBase.divide(new BigDecimal(responsaveis.size()), 2, java.math.RoundingMode.HALF_UP);
        }

        // Se não selecionou ninguém, garantimos que a conta é salva sem responsável
        if (responsaveis.isEmpty()) responsaveis.add(null);

        Conta primeiraContaSalva = null;

        // Para cada pessoa que vai rachar a conta, geramos as parcelas
        for (User responsavelAtual : responsaveis) {
            for (int i = 0; i < meses; i++) {
                Conta novaConta = new Conta();

                // Monta a descrição (ex: Internet (Rateio) (1/12) )
                String desc = conta.getDescricao();
                if (isRateio) desc += " (Rateio)";
                if (meses > 1) desc += " (" + (i + 1) + "/" + meses + ")";

                novaConta.setDescricao(desc);
                novaConta.setValor(valorBase);
                novaConta.setTipo(conta.getTipo());
                novaConta.setCategoria(conta.getCategoria());
                novaConta.setEscopo(conta.getEscopo());
                novaConta.setPrioridade(conta.getPrioridade());
                novaConta.setFrequencia(conta.getFrequencia());
                novaConta.setComprovante(conta.getComprovante());
                novaConta.setResponsavel(responsavelAtual); // Associa o pedaço da conta a esta pessoa
                novaConta.setDataVencimento(conta.getDataVencimento().plusMonths(i));

                // Status de pagamento da primeira parcela
                if (i == 0) {
                    novaConta.setPaga(conta.isPaga());
                    novaConta.setDataPagamento(conta.isPaga() && conta.getDataPagamento() == null ? LocalDate.now() : conta.getDataPagamento());
                    novaConta.setAsset(conta.getAsset());
                    
                    // Sincronização de Saldo se estiver paga
                    if (novaConta.isPaga() && novaConta.getAsset() != null) {
                        atualizarSaldoAtivo(novaConta.getAsset(), novaConta.getValor(), novaConta.getTipo());
                    }
                } else {
                    novaConta.setPaga(false);
                    novaConta.setAsset(conta.getAsset()); // Vincula o ativo também às futuras parcelas, mas não atualiza saldo ainda
                }

                Conta salva = contaRepository.save(novaConta);
                if (primeiraContaSalva == null) primeiraContaSalva = salva;
            }
        }

        return primeiraContaSalva;
    }
    // Busca uma conta pelo ID para poder editá-la e verifica a posse
    public Conta buscarPorId(Long id, User responsavel) {
        return contaRepository.findById(id)
                .filter(c -> c.getResponsavel() != null && c.getResponsavel().getId().equals(responsavel.getId()) || responsavel.getRole().equals("ROLE_ADMIN"))
                .orElse(null);
    }

    public List<Conta> listarTodas(User responsavel) {
        if ("ROLE_ADMIN".equals(responsavel.getRole())) return contaRepository.findAll();
        // Fallback for Admin, otherwise empty since we don't have a findByResponsavel global list method yet
        return new ArrayList<>(); // You might want to create a findByResponsavel in Repo if needed, but we rarely list ALL accounts without filters.
    }

    // Método perfeito para popular o seu dashboard com as contas do mês atual!
    public List<Conta> listarContasDoMesAtual(User responsavel) {
        LocalDate hoje = LocalDate.now();
        return contaRepository.findByResponsavelAndMesEAno(responsavel, hoje.getMonthValue(), hoje.getYear());
    }

    // Calcula o total de saídas (despesas) pendentes
    public BigDecimal calcularTotalDespesasPendentes(User responsavel) {
        List<Conta> pendentes = contaRepository.findByResponsavelAndPagaAndTipo(responsavel, false, TipoTransacao.DESPESA);
        return pendentes.stream()
                .map(Conta::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Você pode adicionar mais cálculos aqui (ex: Saldo Geral, Total de Entradas, etc.)


    // Calcula tudo o que entrou no mês
    public BigDecimal calcularTotalEntradasMes(User responsavel) {
        return listarContasDoMesAtual(responsavel).stream()
                .filter(c -> c.getTipo() == TipoTransacao.RECEITA)
                .map(Conta::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Calcula tudo o que já foi efetivamente pago no mês
    public BigDecimal calcularTotalSaidasPagasMes(User responsavel) {
        return listarContasDoMesAtual(responsavel).stream()
                .filter(c -> c.getTipo() == TipoTransacao.DESPESA && c.isPaga())
                .map(Conta::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // O Saldo é a diferença entre Entradas e Saídas Pagas
    public BigDecimal calcularSaldoAtual(User responsavel) {
        return calcularTotalEntradasMes(responsavel).subtract(calcularTotalSaidasPagasMes(responsavel));
    }

    // Traz a lista das últimas transações
    public List<Conta> listarUltimasTransacoes(User responsavel) {
        return contaRepository.findTop5ByResponsavelAndPagaTrueOrderByDataPagamentoDesc(responsavel);
    }

    // Lista apenas as contas em atraso
    public List<Conta> listarContasAtrasadas(User responsavel) {
        return contaRepository.findByResponsavelAndPagaFalseAndDataVencimentoBeforeOrderByDataVencimentoAsc(responsavel, LocalDate.now());
    }

    // Lista as contas que estão para vencer
    public List<Conta> listarContasAVencer(User responsavel) {
        return contaRepository.findByResponsavelAndPagaFalseAndDataVencimentoGreaterThanEqualOrderByDataVencimentoAsc(responsavel, LocalDate.now());
    }

    // Lista todas as contas pendentes (usado para saber "de quem é qual conta")
    public List<Conta> listarTodasPendentes(User responsavel) {
        return contaRepository.findByResponsavelAndPagaFalseOrderByDataVencimentoAsc(responsavel);
    }


    // Calcula as entradas e saídas dos últimos 6 meses para o gráfico
    // OPTIMIZED: 1 query de intervalo + agrupamento em memória (antes: 6 queries sequenciais)
    public List<Map<String, Object>> obterFluxoCaixaUltimos6Meses(User responsavel) {
        return obterFluxoCaixaUltimos6Meses(responsavel, null);
    }

    public List<Map<String, Object>> obterFluxoCaixaUltimos6Meses(User responsavel, EscopoTransacao escopo) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = hoje.minusMonths(5).withDayOfMonth(1);
        LocalDate fim = hoje.withDayOfMonth(hoje.lengthOfMonth());

        // Uma única query cobrindo o intervalo de 6 meses
        List<Conta> todasContas = contaRepository.findByResponsavelAndPeriodo(responsavel, inicio, fim).stream()
                .filter(c -> escopo == null || c.getEscopo() == escopo)
                .toList();

        // Pré-agrupa por YearMonth em memória para acesso O(1) no loop
        Map<java.time.YearMonth, List<Conta>> porMes = todasContas.stream()
                .filter(c -> c.isPaga() && c.getDataPagamento() != null)
                .collect(Collectors.groupingBy(c -> java.time.YearMonth.from(c.getDataPagamento())));

        List<Map<String, Object>> fluxo = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate dataBase = hoje.minusMonths(i);
            java.time.YearMonth ym = java.time.YearMonth.from(dataBase);
            List<Conta> contasDoMes = porMes.getOrDefault(ym, java.util.Collections.emptyList());

            BigDecimal entradas = contasDoMes.stream()
                    .filter(c -> c.getTipo() == TipoTransacao.RECEITA)
                    .map(Conta::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal saidas = contasDoMes.stream()
                    .filter(c -> c.getTipo() == TipoTransacao.DESPESA)
                    .map(Conta::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String nomeMes = dataBase.getMonth().getDisplayName(TextStyle.SHORT, new Locale("pt", "BR"));
            nomeMes = nomeMes.substring(0, 1).toUpperCase() + nomeMes.substring(1);

            Map<String, Object> mesData = new HashMap<>();
            mesData.put("mes", nomeMes);
            mesData.put("entrada", entradas);
            mesData.put("saida", saidas);
            fluxo.add(mesData);
        }
        return fluxo;
    }

    public Map<String, BigDecimal> obterGastosPorMoradorMesAtual(User responsavel) {
        return listarContasDoMesAtual(responsavel).stream()
                .filter(c -> c.getTipo() == TipoTransacao.DESPESA)
                .collect(Collectors.groupingBy(
                        c -> c.getResponsavel() != null ? c.getResponsavel().getNomeCompleto() : "Sen responsable",
                        Collectors.reducing(BigDecimal.ZERO, Conta::getValor, BigDecimal::add)
                ));
    }

    public Map<String, BigDecimal> obterGastosPorCategoriaMesAtual(User responsavel) {
        return obterGastosPorCategoriaMesAtual(responsavel, null);
    }

    public Map<String, BigDecimal> obterGastosPorCategoriaMesAtual(User responsavel, EscopoTransacao escopo) {
        return listarContasDoMesAtual(responsavel).stream()
                .filter(c -> escopo == null || c.getEscopo() == escopo)
                .filter(c -> c.getTipo() == TipoTransacao.DESPESA)
                .collect(Collectors.groupingBy(
                        c -> c.getCategoria() != null ? c.getCategoria().getNome() : "Sen Categoría",
                        Collectors.reducing(BigDecimal.ZERO, Conta::getValor, BigDecimal::add)
                ));
    }

    // Calcula os dados exatos para desenhar o gráfico Donut em SVG
    public Map<String, Object> obterDadosDonutMesAtual(User responsavel) {
        return obterDadosDonutMesAtual(responsavel, null);
    }

    public Map<String, Object> obterDadosDonutMesAtual(User responsavel, EscopoTransacao escopo) {
        // Pegamos todas as contas de saída do mês
        List<Conta> contasSaida = listarContasDoMesAtual(responsavel).stream()
                .filter(c -> escopo == null || c.getEscopo() == escopo)
                .filter(c -> c.getTipo() == TipoTransacao.DESPESA)
                .toList();

        BigDecimal totalGastos = contasSaida.stream().map(Conta::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> fatias = new ArrayList<>();
        Map<String, Object> resultado = new HashMap<>();

        if (totalGastos.compareTo(BigDecimal.ZERO) == 0) {
            resultado.put("total", BigDecimal.ZERO);
            resultado.put("fatias", fatias);
            return resultado;
        }

        // Agrupar e somar por categoria
        Map<String, Map<String, Object>> grupos = new HashMap<>();
        for (Conta c : contasSaida) {
            String catNome = c.getCategoria() != null ? c.getCategoria().getNome() : "Outros";
            String catCor = c.getCategoria() != null && c.getCategoria().getCorHexadecimal() != null ? c.getCategoria().getCorHexadecimal() : "#94a3b8";

            grupos.putIfAbsent(catNome, new HashMap<>(Map.of("nome", catNome, "cor", catCor, "valor", BigDecimal.ZERO)));
            BigDecimal valAtual = (BigDecimal) grupos.get(catNome).get("valor");
            grupos.get(catNome).put("valor", valAtual.add(c.getValor()));
        }

        // Ordenar do maior gasto para o menor
        List<Map<String, Object>> listaOrdenada = new ArrayList<>(grupos.values());
        listaOrdenada.sort((m1, m2) -> ((BigDecimal) m2.get("valor")).compareTo((BigDecimal) m1.get("valor")));

        // Cálculos para o traçado SVG (Circunferência base é 314.16 para um raio de 50)
        double circumference = 314.159;
        double currentOffset = 0.0;

        for (Map<String, Object> fatia : listaOrdenada) {
            BigDecimal valor = (BigDecimal) fatia.get("valor");
            double percentual = valor.doubleValue() / totalGastos.doubleValue();
            double dash = percentual * circumference;

            fatia.put("percentualText", Math.round(percentual * 100) + "%");
            // Usamos Locale.US para garantir que o SVG receba pontos em vez de vírgulas
            fatia.put("dasharray", String.format(Locale.US, "%.1f %.1f", dash, circumference));
            fatia.put("dashoffset", String.format(Locale.US, "%.1f", -currentOffset));

            currentOffset += dash;
        }

        resultado.put("total", totalGastos);
        resultado.put("fatias", listaOrdenada);
        return resultado;
    }

    public List<Conta> listarHistoricoTransacoes(User responsavel) {
        return contaRepository.findTop100ByResponsavelAndPagaTrueOrderByDataPagamentoDesc(responsavel);
    }
    // Remove uma conta permanentemente da base de dados (IDOR Check included via Responsavel)
    public void excluir(Long id, User responsavel) {
        Conta conta = buscarPorId(id, responsavel);
        if (conta != null) {
            // Se estava paga, estorna o valor do ativo (Opcional, mas recomendado para consistência)
            if (conta.isPaga() && conta.getAsset() != null) {
                // Estorno: se era despesa, soma de volta. Se era receita, subtrai.
                TipoTransacao tipoInverso = conta.getTipo() == TipoTransacao.RECEITA ? TipoTransacao.DESPESA : TipoTransacao.RECEITA;
                atualizarSaldoAtivo(conta.getAsset(), conta.getValor(), tipoInverso);
            }
            contaRepository.deleteById(id);
        }
    }

    private void atualizarSaldoAtivo(Asset asset, BigDecimal valor, TipoTransacao tipo) {
        if (asset instanceof BankAccountAsset ba) {
            BigDecimal saldoAtual = ba.getBalance() != null ? ba.getBalance() : BigDecimal.ZERO;
            if (tipo == TipoTransacao.RECEITA) {
                ba.setBalance(saldoAtual.add(valor));
            } else {
                ba.setBalance(saldoAtual.subtract(valor));
            }
            // Também atualizamos o estimatedValue para manter o snapshot correto
            ba.setEstimatedValue(ba.getBalance());
            assetRepository.save(ba);
        } else {
            // Outros tipos de ativos podem ter lógica similar ou apenas atualizar o estimatedValue
            BigDecimal valorAtual = asset.getEstimatedValue() != null ? asset.getEstimatedValue() : BigDecimal.ZERO;
            if (tipo == TipoTransacao.RECEITA) {
                asset.setEstimatedValue(valorAtual.add(valor));
            } else {
                asset.setEstimatedValue(valorAtual.subtract(valor));
            }
            assetRepository.save(asset);
        }
    }
    // --- API Methods ---

    public Page<ContaResponse> listar(String email, String status, String escopo, Pageable pageable) {
        User user = findUser(email);
        EscopoTransacao escopoFilter = parseEnum(EscopoTransacao.class, escopo, "escopo");
        if (escopoFilter != null) {
            espacoFinanceiroService.validarAcesso(user, escopoFilter);
        }
        if (status != null && "ATRASADO".equalsIgnoreCase(status.trim())) {
            return contaRepository.findOverdueForApi(user, escopoFilter, LocalDate.now(), pageable)
                    .map(this::toResponse);
        }
        StatusTransacao statusFilter = parseEnum(StatusTransacao.class, status, "status");
        return contaRepository.findForApi(user, statusFilter, escopoFilter, pageable).map(this::toResponse);
    }

    public ContaResponse buscarPorId(Long id, String email) {
        User user = findUser(email);
        Conta conta = contaRepository.findByIdAndResponsavelId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada"));
        return toResponse(conta);
    }

    @Transactional
    public ContaResponse criar(ContaRequest request, MultipartFile comprovante, String email) {
        User user = findUser(email);
        espacoFinanceiroService.validarAcesso(user, request.getEscopo());
        Categoria categoria = categoriaService.validarNoEscopo(request.getCategoriaId(), request.getEscopo());

        Conta conta = new Conta();
        conta.setDescricao(request.getDescricao());
        conta.setValor(request.getValor());
        conta.setDataVencimento(request.getDataVencimento());
        conta.setTipo(request.getTipo());
        conta.setEscopo(request.getEscopo());
        conta.setFrequencia(request.getFrequencia());
        conta.setPrioridade(request.getPrioridade());
        conta.setCategoria(categoria);
        conta.setResponsavel(user);
        conta.setAsset(resolveAsset(request.getAssetId(), user));
        
        return toResponse(salvar(conta));
    }

    @Transactional
    public ContaResponse atualizar(Long id, ContaRequest request, MultipartFile comprovante, String email) {
        User user = findUser(email);
        espacoFinanceiroService.validarAcesso(user, request.getEscopo());
        Conta conta = contaRepository.findByIdAndResponsavelId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada"));

        BigDecimal valorAnterior = conta.getValor();
        TipoTransacao tipoAnterior = conta.getTipo();
        Asset assetAnterior = conta.getAsset();

        Categoria categoria = categoriaService.validarNoEscopo(request.getCategoriaId(), request.getEscopo());

        conta.setDescricao(request.getDescricao());
        conta.setValor(request.getValor());
        conta.setDataVencimento(request.getDataVencimento());
        conta.setTipo(request.getTipo());
        conta.setEscopo(request.getEscopo());
        conta.setFrequencia(request.getFrequencia());
        conta.setPrioridade(request.getPrioridade());
        conta.setCategoria(categoria);
        if (request.getAssetId() != null) {
            conta.setAsset(resolveAsset(request.getAssetId(), user));
        }

        if (conta.isPaga()) {
            if (conta.getAsset() == null) {
                throw new BadRequestException("Uma transação paga precisa estar vinculada a uma conta ou ativo");
            }
            if (assetAnterior != null) {
                atualizarSaldoAtivo(assetAnterior, valorAnterior, inverter(tipoAnterior));
            }
            atualizarSaldoAtivo(conta.getAsset(), conta.getValor(), conta.getTipo());
        }

        return toResponse(contaRepository.save(conta));
    }

    @Transactional
    public ContaResponse pagar(Long id, String email) {
        return pagar(id, null, email);
    }

    @Transactional
    public ContaResponse pagar(Long id, UUID assetId, String email) {
        User user = findUser(email);
        Conta conta = contaRepository.findByIdAndResponsavelId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada"));
        if (conta.isPaga()) {
            return toResponse(conta);
        }
        if (assetId != null) {
            conta.setAsset(resolveAsset(assetId, user));
        }
        if (conta.getAsset() == null) {
            throw new BadRequestException("Informe assetId para registrar o pagamento nesta conta");
        }
        conta.setPaga(true);
        conta.setDataPagamento(LocalDate.now());
        conta.setStatus(StatusTransacao.PAGO);
        atualizarSaldoAtivo(conta.getAsset(), conta.getValor(), conta.getTipo());
        return toResponse(contaRepository.save(conta));
    }

    @Transactional
    public void excluir(Long id, String email) {
        User user = findUser(email);
        Conta conta = contaRepository.findByIdAndResponsavelId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada"));
        excluir(conta.getId(), user);
    }

    @Transactional
    public List<ContaResponse> importarLote(List<ContaRequest> transacoes, String email) {
        return transacoes.stream()
                .map(req -> criar(req, null, email))
                .collect(Collectors.toList());
    }

    private User findUser(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private Asset resolveAsset(UUID assetId, User user) {
        if (assetId == null) {
            return null;
        }
        return assetRepository.findByIdAndUserId(assetId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ativo não encontrado"));
    }

    private TipoTransacao inverter(TipoTransacao tipo) {
        return tipo == TipoTransacao.RECEITA ? TipoTransacao.DESPESA : TipoTransacao.RECEITA;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Valor inválido para " + field + ": " + value);
        }
    }

    public ContaResponse toResponse(Conta c) {
        return ContaResponse.builder()
                .id(c.getId())
                .descricao(c.getDescricao())
                .valor(c.getValor())
                .dataVencimento(c.getDataVencimento())
                .dataPagamento(c.getDataPagamento())
                .paga(c.isPaga())
                .tipo(c.getTipo())
                .status(c.getStatus())
                .escopo(c.getEscopo())
                .frequencia(c.getFrequencia())
                .prioridade(c.getPrioridade())
                .categoriaNome(c.getCategoria() != null ? c.getCategoria().getNome() : null)
                .comprovante(c.getComprovante())
                .assetId(c.getAsset() != null ? c.getAsset().getId() : null)
                .build();
    }
}
