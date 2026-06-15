package lucas.basemodel.modules.financeiro.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lucas.basemodel.modules.financeiro.enums.*;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.wealth.models.Asset;
import lucas.basemodel.core.config.DeterministicEncryptionConverter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@NoArgsConstructor   // Gera o construtor vazio exigido pelo JPA/Hibernate
@AllArgsConstructor  // Gera o construtor com todos os argumentos exigido pelo @Builder
@Entity
@Table(name = "contas")
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Prioridade prioridade = Prioridade.MEDIA;

    @NotBlank(message = "A descrição é obrigatória")
    @Column(nullable = false)
    @Convert(converter = DeterministicEncryptionConverter.class)
    private String descricao;

    @NotNull(message = "O valor é obrigatório")
    @Positive(message = "O valor deve ser maior que zero")
    @Column(nullable = false)
    private BigDecimal valor;

    @NotNull(message = "A data de vencimento é obrigatória")
    @Column(nullable = false)
    private LocalDate dataVencimento;

    private LocalDate dataPagamento;

    private boolean paga = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTransacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusTransacao status = StatusTransacao.PENDENTE;

    @Column(precision = 10, scale = 2)
    private BigDecimal valorPrevisto;

    @Column(precision = 10, scale = 2)
    private BigDecimal valorRealizado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transacao_recorrente_id")
    private TransacaoRecorrente transacaoRecorrente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private User responsavel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Transient
    @Builder.Default
    private Integer mesesRecorrencia = 1;

    // Campo temporário para receber os moradores selecionados nos checkboxes
    @Transient
    @Builder.Default
    private java.util.List<User> responsaveisRateio = new java.util.ArrayList<>();

    private String comprovante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EscopoTransacao escopo = EscopoTransacao.CASA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT 'AVULSA'")
    @Builder.Default
    private Frequencia frequencia = Frequencia.AVULSA;

    // Getters e Setters
    public Frequencia getFrequencia() { return frequencia; }
    public void setFrequencia(Frequencia frequencia) { this.frequencia = frequencia; }
    public EscopoTransacao getEscopo() { return escopo; }
    public void setEscopo(EscopoTransacao escopo) { this.escopo = escopo; }
    public String getComprovante() { return comprovante; }
    public void setComprovante(String comprovante) { this.comprovante = comprovante; }
    public java.util.List<User> getResponsaveisRateio() { return responsaveisRateio; }
    public void setResponsaveisRateio(java.util.List<User> responsaveisRateio) { this.responsaveisRateio = responsaveisRateio; }
    public Integer getMesesRecorrencia() { return mesesRecorrencia; }
    public void setMesesRecorrencia(Integer mesesRecorrencia) { this.mesesRecorrencia = mesesRecorrencia; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDate getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(LocalDate dataVencimento) { this.dataVencimento = dataVencimento; }
    public LocalDate getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDate dataPagamento) { this.dataPagamento = dataPagamento; }
    public boolean isPaga() { return paga; }
    public void setPaga(boolean paga) { this.paga = paga; }
    public TipoTransacao getTipo() { return tipo; }
    public void setTipo(TipoTransacao tipo) { this.tipo = tipo; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public User getResponsavel() { return responsavel; }
    public void setResponsavel(User responsavel) { this.responsavel = responsavel; }
    public Prioridade getPrioridade() { return prioridade; }
    public void setPrioridade(Prioridade prioridade) { this.prioridade = prioridade; }
    public StatusTransacao getStatus() { return status; }
    public void setStatus(StatusTransacao status) { this.status = status; }
    public BigDecimal getValorPrevisto() { return valorPrevisto; }
    public void setValorPrevisto(BigDecimal valorPrevisto) { this.valorPrevisto = valorPrevisto; }
    public BigDecimal getValorRealizado() { return valorRealizado; }
    public void setValorRealizado(BigDecimal valorRealizado) { this.valorRealizado = valorRealizado; }
    public TransacaoRecorrente getTransacaoRecorrente() { return transacaoRecorrente; }
    public void setTransacaoRecorrente(TransacaoRecorrente transacaoRecorrente) { this.transacaoRecorrente = transacaoRecorrente; }
    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }

}