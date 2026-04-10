package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.wealth.models.Asset;
import lucas.basemodel.modules.wealth.models.BankAccountAsset;
import lucas.basemodel.modules.wealth.repositories.AssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class ContaService {

    private final ContaRepository contaRepository;
    private final AssetRepository assetRepository;

    public ContaService(ContaRepository contaRepository, AssetRepository assetRepository) {
        this.contaRepository = contaRepository;
        this.assetRepository = assetRepository;
    }

    @Transactional
    public Conta salvar(Conta conta) {

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
    public List<Map<String, Object>> obterFluxoCaixaUltimos6Meses(User responsavel) {
        List<Map<String, Object>> fluxo = new ArrayList<>();
        LocalDate hoje = LocalDate.now();

        // O loop vai de 5 até 0 (ex: volta 5 meses atrás, depois 4, 3... até o mês atual)
        for (int i = 5; i >= 0; i--) {
            LocalDate dataBase = hoje.minusMonths(i);
            int mes = dataBase.getMonthValue();
            int ano = dataBase.getYear();

            // Busca as contas daquele mês/ano específico
            List<Conta> contasDoMes = contaRepository.findByResponsavelAndMesEAno(responsavel, mes, ano);

            // Soma apenas o que já foi PAGO/RECEBIDO
            BigDecimal entradas = contasDoMes.stream()
                    .filter(c -> c.isPaga() && c.getTipo() == TipoTransacao.RECEITA)
                    .map(Conta::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal saidas = contasDoMes.stream()
                    .filter(c -> c.isPaga() && c.getTipo() == TipoTransacao.DESPESA)
                    .map(Conta::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Pega o nome do mês abreviado (ex: jan, fev) e põe a 1ª letra maiúscula
            String nomeMes = dataBase.getMonth().getDisplayName(TextStyle.SHORT, new Locale("pt", "BR"));
            nomeMes = nomeMes.substring(0, 1).toUpperCase() + nomeMes.substring(1);

            // Guarda os dados num Mapa para enviar ao Javascript
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
        return listarContasDoMesAtual(responsavel).stream()
                .filter(c -> c.getTipo() == TipoTransacao.DESPESA)
                .collect(Collectors.groupingBy(
                        c -> c.getCategoria() != null ? c.getCategoria().getNome() : "Sen Categoría",
                        Collectors.reducing(BigDecimal.ZERO, Conta::getValor, BigDecimal::add)
                ));
    }

    // Calcula os dados exatos para desenhar o gráfico Donut em SVG
    public Map<String, Object> obterDadosDonutMesAtual(User responsavel) {
        // Pegamos todas as contas de saída do mês
        List<Conta> contasSaida = listarContasDoMesAtual(responsavel).stream()
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
}