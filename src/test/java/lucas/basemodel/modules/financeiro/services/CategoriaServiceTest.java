package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.core.exceptions.BadRequestException;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.financeiro.repositories.CategoriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock CategoriaRepository repository;

    @Test
    void rejeitaCategoriaDeOutroEspaco() {
        Categoria categoriaCasa = new Categoria();
        categoriaCasa.setNome("Energia elétrica");
        categoriaCasa.setEscopo(EscopoTransacao.CASA);
        CategoriaService service = new CategoriaService(repository);

        assertThrows(BadRequestException.class,
                () -> service.validarNoEscopo(categoriaCasa, EscopoTransacao.PESSOAL));
        assertDoesNotThrow(() -> service.validarNoEscopo(categoriaCasa, EscopoTransacao.CASA));
    }

    @Test
    void reutilizaCategoriaLegadaSemEscopoEmVezDeDuplicarNome() {
        Categoria legada = new Categoria();
        legada.setNome("Despesas Essenciais");
        when(repository.findByNomeIgnoreCaseAndEscopo(
                "Despesas Essenciais", EscopoTransacao.PESSOAL)).thenReturn(Optional.empty());
        when(repository.findFirstByNomeIgnoreCase("Despesas Essenciais")).thenReturn(Optional.of(legada));
        when(repository.save(legada)).thenReturn(legada);
        CategoriaService service = new CategoriaService(repository);

        Categoria categoria = service.buscarOuCriarPorNome("Despesas Essenciais");

        assertSame(legada, categoria);
        assertSame(EscopoTransacao.PESSOAL, categoria.getEscopo());
        verify(repository).save(legada);
    }
}
