package lucas.basemodel.modules.financeiro.models;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ContaIndexesTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    void contasTemOsTresIndicesDeConsulta() {
        List<String> nomes = jdbc.queryForList(
                "SELECT LOWER(INDEX_NAME) FROM INFORMATION_SCHEMA.INDEXES WHERE LOWER(TABLE_NAME) = 'contas'",
                String.class);

        assertTrue(nomes.contains("idx_contas_resp_vencimento"), "faltou idx_contas_resp_vencimento em " + nomes);
        assertTrue(nomes.contains("idx_contas_resp_paga_pagamento"), "faltou idx_contas_resp_paga_pagamento em " + nomes);
        assertTrue(nomes.contains("idx_contas_resp_escopo_tipo"), "faltou idx_contas_resp_escopo_tipo em " + nomes);
    }
}
