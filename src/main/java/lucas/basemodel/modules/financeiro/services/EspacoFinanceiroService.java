package lucas.basemodel.modules.financeiro.services;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.core.exceptions.BadRequestException;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.enums.SituacaoMoradia;
import lucas.basemodel.modules.financeiro.models.ConfiguracaoFinanceira;
import lucas.basemodel.modules.financeiro.repositories.ConfiguracaoFinanceiraRepository;
import lucas.basemodel.modules.user.User;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

/** Centraliza quais espaços financeiros estão habilitados para cada usuário. */
@Service
@RequiredArgsConstructor
public class EspacoFinanceiroService {

    private final ConfiguracaoFinanceiraRepository configuracaoRepository;

    public void validarAcesso(User user, EscopoTransacao escopo) {
        if (escopo == null) {
            throw new BadRequestException("Informe o espaço financeiro da operação");
        }
        if (!listarPermitidos(user).contains(escopo)) {
            throw new BadRequestException("O espaço " + escopo + " não está habilitado para este perfil");
        }
    }

    public Set<EscopoTransacao> listarPermitidos(User user) {
        EnumSet<EscopoTransacao> permitidos = EnumSet.of(EscopoTransacao.PESSOAL);
        ConfiguracaoFinanceira config = configuracaoRepository.findByUser(user).orElse(null);

        if (config == null || config.getSituacaoMoradia() != SituacaoMoradia.COM_OS_PAIS) {
            permitidos.add(EscopoTransacao.CASA);
        }
        if (config != null && config.isPossuiNegocio()) {
            permitidos.add(EscopoTransacao.NEGOCIO);
        }
        return Set.copyOf(permitidos);
    }
}
