package lucas.basemodel.modules.user;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.UUID;


@Controller
@RequestMapping("/app/settings")
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class UserController {

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;

    @PostMapping("/usuarios/rapido")
    public String criarUsuarioRapido(@RequestParam String username,
                                     @RequestParam String password,
                                     @RequestParam String role,
                                     Model model, HttpServletResponse response) {

        if (repository.findByEmail(username).isPresent()) {
            model.addAttribute("mensagemErro", "Este nome de utilizador já existe.");
            return "fragmentos/toast :: erroToast";
        }

        User u = new User();

        u.setUsername(username.toLowerCase());
        u.setPassword(encoder.encode(password));
        u.setRole(role);
        u.setNomeCompleto(username); // ou um param separado se quiser

        u.setEmail(username.toLowerCase() + "@email.com");

        repository.save(u);

        model.addAttribute("mensagemSucesso", "Usuário " + username + " criado com sucesso!");
        response.setHeader("HX-Retarget", "#toast-container");
        response.setHeader("HX-Reswap", "innerHTML");
        return "fragmentos/toast :: sucessoToast";
    }
    @GetMapping("/perfil")
    public String verPerfil(Model model, Principal principal) {
        String email = principal != null ? principal.getName() : "admin";

        // Mudei de orElse(null) para orElseThrow()
        User user = repository.findByEmail(email).orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));

        model.addAttribute("user", user);
        return "user/perfil";
    }

    @PostMapping("/perfil/foto")
    public String atualizarFoto(@RequestParam("foto") MultipartFile file,
                                Principal principal,
                                RedirectAttributes attributes) {
        if (file.isEmpty()) {
            attributes.addFlashAttribute("mensagemErro", "Por favor, selecione um ficheiro.");
            return "redirect:/app/settings/perfil";
        }

        try {
            String email = principal != null ? principal.getName() : "admin";
            User user = repository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));

            // 1. Definir o diretório de destino na raiz do projeto (fora de 'target' para não apagar)
            String uploadDir = "uploads/perfis/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 2. Gerar um nome único para o ficheiro para evitar sobreposições
            String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + ext;
            Path filePath = uploadPath.resolve(fileName);

            // 3. Gravar o ficheiro
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 4. Atualizar o caminho da foto na entidade (com o prefixo mapeado no WebConfig)
            user.setFotoPerfil("/uploads/perfis/" + fileName);
            repository.save(user);

            attributes.addFlashAttribute("mensagemSucesso", "Foto de perfil atualizada com sucesso!");
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro", "Ocorreu um erro ao gravar a imagem.");
            log.error("Erro ao atualizar foto de perfil: ", e);
        }

        return "redirect:/app/settings/perfil";
    }

    @PostMapping("/user/config-dna")
    public String configurarDna(@RequestParam("tipoPerfilFinanceiro") String tipo,
                                @RequestParam("metaPoupancaMensal") Integer meta,
                                @RequestParam("tetoGastosEssenciais") Integer teto,
                                Principal principal,
                                RedirectAttributes attributes) {
        try {
            String email = principal.getName();
            User user = repository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));

            user.setTipoPerfilFinanceiro(tipo);
            user.setMetaPoupancaMensal(new java.math.BigDecimal(meta));
            user.setTetoGastosEssenciais(new java.math.BigDecimal(teto));
            
            repository.save(user);
            attributes.addFlashAttribute("mensagemSucesso", "DNA Financeiro atualizado! A IA agora será mais precisa.");
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro", "Erro ao atualizar perfil.");
        }
        return "redirect:/app/dashboard";
    }
}