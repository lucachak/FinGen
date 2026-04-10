package lucas.basemodel.core.config;

import lombok.RequiredArgsConstructor;
import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import lucas.basemodel.modules.wealth.enums.*;
import lucas.basemodel.modules.wealth.models.*;
import lucas.basemodel.modules.wealth.repositories.AssetRepository;
import lucas.basemodel.modules.wealth.services.WealthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class WealthDataSeeder {

    private final WealthService wealthService;
    private final AssetRepository assetRepository;
    private final UsuarioRepository usuarioRepository;

    @Bean
    public CommandLineRunner initWealthData() {
        return args -> {
            Optional<User> lucasOpt = usuarioRepository.findByEmail("lucas@admin.com");
            if (lucasOpt.isPresent()) {
                // Mock assets generator bypassed. Start with an empty wallet/dashboard!
            }
        };
    }
}
