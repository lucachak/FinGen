package lucas.basemodel.modules.financeiro.repositories;

import jakarta.persistence.EntityManager;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.financeiro.models.Orcamento;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Sem @Transactional de propósito: precisa provar o comportamento fora de uma sessão aberta. */
@SpringBootTest
class OrcamentoLazyTest {

    @Autowired OrcamentoRepository orcamentos;
    @Autowired CategoriaRepository categorias;
    @Autowired UsuarioRepository usuarios;
    @Autowired EntityManager em;
    @Autowired TransactionTemplate tx;

    User user;
    Categoria categoria;
    Orcamento orcamento;

    @BeforeEach
    void criar() {
        String id = "lazy-" + UUID.randomUUID();
        user = usuarios.save(User.builder().email(id + "@t.com").username(id).password("x").role("ROLE_USER").build());
        Categoria c = new Categoria();
        c.setNome(id);
        c.setEscopo(EscopoTransacao.PESSOAL);
        categoria = categorias.save(c);
        orcamento = orcamentos.save(Orcamento.builder()
                .categoria(categoria).responsavel(user).limiteMensal(new BigDecimal("100")).build());
    }

    @AfterEach
    void limpar() {
        orcamentos.deleteById(orcamento.getId());
        categorias.deleteById(categoria.getId());
        usuarios.deleteById(user.getId());
    }

    @Test
    void findByIdNaoCarregaCategoriaAteSerNecessaria() {
        boolean inicializada = tx.execute(status -> {
            em.clear();
            Orcamento carregado = orcamentos.findById(orcamento.getId()).orElseThrow();
            return Hibernate.isInitialized(carregado.getCategoria());
        });

        assertFalse(inicializada, "categoria deveria ser LAZY e não vir junto com o orçamento");
    }

    @Test
    void findByResponsavelCarregaCategoriaSemSessaoAberta() {
        List<Orcamento> lista = orcamentos.findByResponsavel(user);

        assertEquals(1, lista.size());
        assertEquals(categoria.getNome(), lista.get(0).getCategoria().getNome());
    }
}
