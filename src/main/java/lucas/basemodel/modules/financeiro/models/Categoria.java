package lucas.basemodel.modules.financeiro.models;

import jakarta.persistence.*;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.NaturezaCategoria;

@Entity
@Table(name = "categorias", uniqueConstraints = {
        @UniqueConstraint(name = "uk_categoria_nome_escopo", columnNames = {"nome", "escopo"})
})
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    /**
     * Espaço financeiro ao qual a categoria pertence. Mantido nullable no banco
     * somente para permitir a migração das instalações existentes; novos registros
     * são sempre validados pelo serviço.
     */
    @Enumerated(EnumType.STRING)
    private EscopoTransacao escopo;

    private String corHexadecimal; // Para deixar o dashboard bonito

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NaturezaCategoria natureza = NaturezaCategoria.ESTILO_VIDA; // Padrão

    @Column(nullable = true)
    private String icone = "tag"; // Ícone padrão Lucide

    public Categoria() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public EscopoTransacao getEscopo() { return escopo; }
    public void setEscopo(EscopoTransacao escopo) { this.escopo = escopo; }
    public String getCorHexadecimal() { return corHexadecimal; }
    public void setCorHexadecimal(String corHexadecimal) { this.corHexadecimal = corHexadecimal; }
    public NaturezaCategoria getNatureza() { return natureza; }
    public void setNatureza(NaturezaCategoria natureza) { this.natureza = natureza; }

    public String getIcone() { return icone; }
    public void setIcone(String icone) { this.icone = icone; }
}
