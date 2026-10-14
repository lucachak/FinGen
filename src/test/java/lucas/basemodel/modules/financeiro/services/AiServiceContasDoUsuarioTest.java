package lucas.basemodel.modules.financeiro.services;

import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AiServiceContasDoUsuarioTest {

    private final FinanceAiService financeAi = mock(FinanceAiService.class);
    private final OpenRouterService openRouter = mock(OpenRouterService.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ContaRepository contas = mock(ContaRepository.class);
    private final EspacoFinanceiroService espacos = mock(EspacoFinanceiroService.class);
    private final AiService service = new AiService(financeAi, openRouter, usuarios, contas, espacos);

    @Test
    @SuppressWarnings("unchecked")
    void chatNaoUsaFindAllEEnviaSoContasDoUsuario() {
        User user = User.builder().id(UUID.randomUUID()).email("ai@t.com").build();
        List<Conta> doUsuario = List.of(new Conta(), new Conta());
        when(usuarios.findByEmail("ai@t.com")).thenReturn(Optional.of(user));
        when(contas.findAllByResponsavelOrderByDataVencimentoAsc(user)).thenReturn(doUsuario);
        when(openRouter.chat(any(), any(), any())).thenReturn("ok");

        var resposta = service.chat("oi", "ai@t.com");

        assertEquals("ok", resposta.getReply());
        verify(contas, never()).findAll();
        ArgumentCaptor<List<Conta>> enviadas = ArgumentCaptor.forClass(List.class);
        verify(openRouter).chat(any(), eq(user), enviadas.capture());
        assertEquals(doUsuario, enviadas.getValue());
    }
}
