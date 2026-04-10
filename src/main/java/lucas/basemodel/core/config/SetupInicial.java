package lucas.basemodel.core.config;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lucas.basemodel.modules.financeiro.models.Categoria;
import lucas.basemodel.modules.financeiro.models.Conta;
import lucas.basemodel.modules.financeiro.enums.Prioridade;
import lucas.basemodel.modules.financeiro.enums.TipoTransacao;
import lucas.basemodel.modules.financeiro.enums.NaturezaCategoria;
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

        // 2. Setup das Categorias (Agora mapeadas automaticamente para a sua Natureza)
        if (categoriaRepository.count() == 0) {
            System.out.println("Criando categorias da casa com suas naturezas...");

            Map<NaturezaCategoria, List<String>> categoriasPorNatureza = Map.of(
                    NaturezaCategoria.ESSENCIAL, Arrays.asList(
                            "MORADIA", "CONDOMINIO", "SUPERMERCADO", "TRANSPORTE", "FARMACIA",
                            "SALARIO_ESTAGIO", "NEGOCIO_EBIKE", "FREELANCE_DEV"
                    ),
                    NaturezaCategoria.ESTILO_VIDA, Arrays.asList(
                            "DELIVERY", "MANUTENCAO_MOTO", "ESTUDOS", "ACADEMIA", "CUIDADOS_PESSOAIS", "LAZER", "CINEMA"
                    ),
                    NaturezaCategoria.INVESTIMENTO, Arrays.asList(
                            "INVESTIMENTOS", "RENDIMENTOS"
                    )
            );

            String[] paleta = {"#2563eb", "#16a34a", "#f59e0b", "#7c3aed", "#dc2626", "#0891b2", "#c026d3", "#ea580c"};
            int corIndex = 0;

            for (Map.Entry<NaturezaCategoria, List<String>> entry : categoriasPorNatureza.entrySet()) {
                NaturezaCategoria naturezaAtual = entry.getKey();
                List<String> nomes = entry.getValue();

                for (String nome : nomes) {
                    Categoria cat = new Categoria();

                    String[] palavras = nome.split("_");
                    StringBuilder formatado = new StringBuilder();
                    for (String p : palavras) {
                        formatado.append(p.substring(0, 1).toUpperCase()).append(p.substring(1).toLowerCase()).append(" ");
                    }

                    cat.setNome(formatado.toString().trim());
                    cat.setCorHexadecimal(paleta[corIndex % paleta.length]);
                    cat.setNatureza(naturezaAtual);

                    categoriaRepository.save(cat);
                    corIndex++;
                }
            }
        }
    }
}