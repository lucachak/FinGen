package lucas.basemodel.core.config;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.enums.Prioridade;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.enums.NaturezaCategoria;
import lucas.basemodel.modules.financeiro.enums.EscopoTransacao;
import lucas.basemodel.modules.financeiro.repositories.CategoriaRepository;
import lucas.basemodel.modules.financeiro.repositories.ContaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class SetupInicial implements CommandLineRunner {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final CategoriaRepository categoriaRepository;
    private final ContaRepository contaRepository;

    @Override
    public void run(String... args) throws Exception {
        // Coloque os nomes completos aqui para a IA conseguir bater com o PDF!
        List<String> nomesCompletos = Arrays.asList("Lucas Lucachak");

        for (String nomeCompleto : nomesCompletos) {
            // Extrai apenas o primeiro nome (em minúsculas) para criar o username e o email
            String primeiroNome = nomeCompleto.split(" ")[0].toLowerCase();

            // Define o e-mail base (se for o Lucas, é admin)
            String email = primeiroNome + (primeiroNome.equals("lucas") ? "@admin.com" : "@casa.com");

            if (repository.findByEmail(email).isEmpty()) {
                User user = new User();

                // 1. Username limpo (ex: "Lucas")
                user.setUsername(primeiroNome.substring(0, 1).toUpperCase() + primeiroNome.substring(1));

                // 2. Nome Completo Real (Crucial para a verificação do Extrato PDF!)
                user.setNomeCompleto(nomeCompleto);

                // 3. Senhas e Roles
                String senha = primeiroNome.equals("lucas") ? "lucas" : "123456";
                user.setPassword(passwordEncoder.encode(senha));
                user.setRole(primeiroNome.equals("lucas") ? "ROLE_ADMIN" : "ROLE_USER");
                user.setEmail(email);

                repository.save(user);
            }
        }

        // Setup da conta demo
        if (repository.findByEmail("demo@fingen.com").isEmpty()) {
            User demo = new User();
            demo.setUsername("Demo");
            demo.setNomeCompleto("Conta Demo");
            demo.setPassword(passwordEncoder.encode("demo123"));
            demo.setRole("ROLE_USER");
            demo.setEmail("demo@fingen.com");
            repository.save(demo);
        }

        configurarCategoriasPorEspaco();
    }

    private void configurarCategoriasPorEspaco() {
        Map<String, EscopoTransacao> categoriasLegadas = Map.ofEntries(
                Map.entry("Moradia", EscopoTransacao.CASA),
                Map.entry("Condominio", EscopoTransacao.CASA),
                Map.entry("Supermercado", EscopoTransacao.CASA),
                Map.entry("Transporte", EscopoTransacao.PESSOAL),
                Map.entry("Farmacia", EscopoTransacao.PESSOAL),
                Map.entry("Salario Estagio", EscopoTransacao.PESSOAL),
                Map.entry("Negocio Ebike", EscopoTransacao.NEGOCIO),
                Map.entry("Freelance Dev", EscopoTransacao.PESSOAL),
                Map.entry("Delivery", EscopoTransacao.PESSOAL),
                Map.entry("Manutencao Moto", EscopoTransacao.PESSOAL),
                Map.entry("Estudos", EscopoTransacao.PESSOAL),
                Map.entry("Academia", EscopoTransacao.PESSOAL),
                Map.entry("Cuidados Pessoais", EscopoTransacao.PESSOAL),
                Map.entry("Cinema", EscopoTransacao.PESSOAL),
                Map.entry("Investimentos", EscopoTransacao.PESSOAL),
                Map.entry("Rendimentos", EscopoTransacao.PESSOAL));
        categoriasLegadas.forEach((nome, escopo) -> categoriaRepository.findFirstByNomeIgnoreCase(nome).ifPresent(categoria -> {
            if (categoria.getEscopo() == null) {
                categoria.setEscopo(escopo);
                categoriaRepository.save(categoria);
            }
        }));

        Map<EscopoTransacao, Map<NaturezaCategoria, List<String>>> catalogo = Map.of(
                EscopoTransacao.PESSOAL, Map.of(
                        NaturezaCategoria.ESSENCIAL, List.of("Compras", "Transporte", "Higiene pessoal", "Saúde", "Educação"),
                        NaturezaCategoria.ESTILO_VIDA, List.of("Lazer", "Assinaturas", "Alimentação pessoal"),
                        NaturezaCategoria.INVESTIMENTO, List.of("Investimentos pessoais", "Rendimentos pessoais")),
                EscopoTransacao.CASA, Map.of(
                        NaturezaCategoria.ESSENCIAL, List.of("Água", "Energia elétrica", "Gás", "Mercado", "Aluguel ou financiamento", "Condomínio", "Internet residencial"),
                        NaturezaCategoria.ESTILO_VIDA, List.of("Produtos de limpeza", "Manutenção doméstica")),
                EscopoTransacao.NEGOCIO, Map.of(
                        NaturezaCategoria.ESSENCIAL, List.of("Impostos e taxas", "Manutenção empresarial", "Fornecedores", "Folha e pró-labore", "Contabilidade", "Aluguel comercial"),
                        NaturezaCategoria.ESTILO_VIDA, List.of("Software e serviços", "Marketing", "Logística"))
        );

        String[] paleta = {"#2563eb", "#16a34a", "#f59e0b", "#7c3aed", "#dc2626", "#0891b2", "#c026d3", "#ea580c"};
        int corIndex = 0;
        for (Map.Entry<EscopoTransacao, Map<NaturezaCategoria, List<String>>> espaco : catalogo.entrySet()) {
            for (Map.Entry<NaturezaCategoria, List<String>> grupo : espaco.getValue().entrySet()) {
                for (String nome : grupo.getValue()) {
                    if (categoriaRepository.findByNomeIgnoreCaseAndEscopo(nome, espaco.getKey()).isEmpty()) {
                        Categoria categoria = categoriaRepository.findFirstByNomeIgnoreCase(nome).orElseGet(Categoria::new);
                        categoria.setNome(nome);
                        categoria.setEscopo(espaco.getKey());
                        categoria.setNatureza(grupo.getKey());
                        categoria.setCorHexadecimal(paleta[corIndex % paleta.length]);
                        categoriaRepository.save(categoria);
                    }
                    corIndex++;
                }
            }
        }
    }
}
