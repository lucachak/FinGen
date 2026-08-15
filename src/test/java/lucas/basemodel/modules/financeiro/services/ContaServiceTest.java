package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.core.exceptions.BadRequestException;
import lucas.basemodel.core.exceptions.ResourceNotFoundException;
import lucas.basemodel.modules.financeiro.dto.ContaResponse;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.StatusTransacao;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.repositories.CategoriaRepository;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lucas.basemodel.modules.wealth.models.BankAccountAsset;
import lucas.basemodel.modules.wealth.repositories.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

    @Mock ContaRepository contaRepository;
    @Mock AssetRepository assetRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock CategoriaRepository categoriaRepository;
    @Mock CategoriaService categoriaService;
    @Mock EspacoFinanceiroService espacoFinanceiroService;

    private ContaService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new ContaService(contaRepository, assetRepository, usuarioRepository, categoriaRepository,
                categoriaService, espacoFinanceiroService);
        user = User.builder().id(UUID.randomUUID()).email("user@example.com").role("USER").build();
        lenient().when(usuarioRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void listarDelegatesFilteringAndPaginationToDatabase() {
        var pageable = PageRequest.of(0, 20);
        Conta conta = novaConta();
        when(contaRepository.findForApi(user, StatusTransacao.PENDENTE, EscopoTransacao.CASA, pageable))
                .thenReturn(new PageImpl<>(List.of(conta), pageable, 1));

        var result = service.listar("user@example.com", "pendente", "casa", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(conta.getId(), result.getContent().get(0).getId());
        verify(contaRepository).findForApi(user, StatusTransacao.PENDENTE, EscopoTransacao.CASA, pageable);
        verify(contaRepository, never()).findAll();
    }

    @Test
    void listarRejectsUnknownFilter() {
        var pageable = PageRequest.of(0, 20);
        assertThrows(BadRequestException.class,
                () -> service.listar("user@example.com", "desconhecido", null, pageable));
    }

    @Test
    void listarSupportsComputedOverdueStatus() {
        var pageable = PageRequest.of(0, 20);
        when(contaRepository.findOverdueForApi(user, EscopoTransacao.CASA, LocalDate.now(), pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = service.listar("user@example.com", "atrasado", "casa", pageable);

        assertTrue(result.isEmpty());
        verify(contaRepository).findOverdueForApi(user, EscopoTransacao.CASA, LocalDate.now(), pageable);
    }

    @Test
    void buscarPorIdUsesOwnerScopedQuery() {
        when(contaRepository.findByIdAndResponsavelId(42L, user.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.buscarPorId(42L, "user@example.com"));
        verify(contaRepository).findByIdAndResponsavelId(42L, user.getId());
        verify(contaRepository, never()).findById(any());
    }

    @Test
    void pagarUpdatesBalanceOnceAndIsIdempotent() {
        UUID assetId = UUID.randomUUID();
        BankAccountAsset asset = BankAccountAsset.builder()
                .id(assetId)
                .user(user)
                .balance(new BigDecimal("100.00"))
                .estimatedValue(new BigDecimal("100.00"))
                .build();
        Conta conta = novaConta();
        conta.setAsset(null);

        when(contaRepository.findByIdAndResponsavelId(conta.getId(), user.getId()))
                .thenReturn(Optional.of(conta));
        when(assetRepository.findByIdAndUserId(assetId, user.getId())).thenReturn(Optional.of(asset));
        when(contaRepository.save(conta)).thenReturn(conta);

        ContaResponse first = service.pagar(conta.getId(), assetId, "user@example.com");
        ContaResponse second = service.pagar(conta.getId(), assetId, "user@example.com");

        assertTrue(first.isPaga());
        assertTrue(second.isPaga());
        assertEquals(new BigDecimal("75.00"), asset.getBalance());
        verify(assetRepository, times(1)).save(asset);
        verify(contaRepository, times(1)).save(conta);
    }

    @Test
    void salvarPreservaEspacoAoCriarParcelas() {
        Conta conta = novaConta();
        conta.setId(null);
        conta.setEscopo(EscopoTransacao.NEGOCIO);
        conta.setResponsaveisRateio(new java.util.ArrayList<>(List.of(user)));
        when(contaRepository.save(any(Conta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.salvar(conta);

        ArgumentCaptor<Conta> captor = ArgumentCaptor.forClass(Conta.class);
        verify(contaRepository).save(captor.capture());
        assertEquals(EscopoTransacao.NEGOCIO, captor.getValue().getEscopo());
        verify(categoriaService).validarNoEscopo(conta.getCategoria(), EscopoTransacao.NEGOCIO);
    }

    @Test
    void salvarParaUsuarioRecusaEdicaoDeOutroProprietario() {
        Conta conta = novaConta();
        when(contaRepository.findByIdAndResponsavelId(conta.getId(), user.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.salvarParaUsuario(conta, user));

        verify(contaRepository, never()).save(any());
    }

    @Test
    void salvarParaUsuarioIgnoraProprietarioEnviadoPeloCliente() {
        User outro = User.builder().id(UUID.randomUUID()).email("outro@example.com").build();
        Conta conta = novaConta();
        conta.setId(null);
        conta.setResponsavel(outro);
        conta.setResponsaveisRateio(new java.util.ArrayList<>(List.of(outro)));
        when(contaRepository.save(any(Conta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.salvarParaUsuario(conta, user);

        ArgumentCaptor<Conta> captor = ArgumentCaptor.forClass(Conta.class);
        verify(contaRepository).save(captor.capture());
        assertEquals(user.getId(), captor.getValue().getResponsavel().getId());
    }

    private Conta novaConta() {
        Conta conta = new Conta();
        conta.setId(10L);
        conta.setDescricao("Internet");
        conta.setValor(new BigDecimal("25.00"));
        conta.setDataVencimento(LocalDate.now());
        conta.setTipo(TipoTransacao.DESPESA);
        conta.setStatus(StatusTransacao.PENDENTE);
        conta.setEscopo(EscopoTransacao.CASA);
        conta.setResponsavel(user);
        return conta;
    }
}
