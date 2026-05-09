package lucas.basemodel.web;

import lucas.basemodel.modules.user.User;
import lucas.basemodel.modules.user.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@lombok.extern.slf4j.Slf4j
@Controller
@RequestMapping("/app/settings/moradores")
public class MoradorController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public MoradorController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping({"", "/"})
    public String listarMoradores(Model model) {
        model.addAttribute("activeMenu", "moradores");
        model.addAttribute("moradores", usuarioRepository.findAllByAtivoTrue());
        return "moradores/lista";
    }

    @GetMapping("/novo")
    public String novoMorador(Model model) {
        model.addAttribute("activeMenu", "moradores");
        model.addAttribute("morador", new User());
        return "moradores/form";
    }

    @GetMapping("/editar/{id}")
    public String editarMorador(@PathVariable UUID id, Model model) {
        model.addAttribute("activeMenu", "moradores");

        User morador = usuarioRepository.findById(id).orElse(null);
        if (morador == null) {
            return "redirect:/app/settings/moradores";
        }

        model.addAttribute("morador", morador);
        return "moradores/form";
    }

    @PostMapping("/salvar")
    public String salvarMorador(User formUser, @RequestParam(value = "fileFoto", required = false) org.springframework.web.multipart.MultipartFile fileFoto) {
        String novaFoto = null;
        if (fileFoto != null && !fileFoto.isEmpty()) {
            try {
                String uploadDir = "uploads/perfis/";
                java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
                if (!java.nio.file.Files.exists(uploadPath)) {
                    java.nio.file.Files.createDirectories(uploadPath);
                }
                String originalFilename = fileFoto.getOriginalFilename();
                String ext = originalFilename != null && originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
                String fileName = java.util.UUID.randomUUID().toString() + ext;
                java.nio.file.Path filePath = uploadPath.resolve(fileName);
                try (java.io.InputStream inputStream = fileFoto.getInputStream()) {
                    java.nio.file.Files.copy(inputStream, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                novaFoto = "/uploads/perfis/" + fileName;
            } catch (Exception e) {
                log.error("Erro ao salvar foto do morador: ", e);
            }
        }

        if (formUser.getId() == null) {
            // É um NOVO morador
            formUser.setPassword(passwordEncoder.encode(formUser.getPassword()));
            formUser.setRole("ROLE_USER"); // Papel padrão
            if (novaFoto != null) formUser.setFotoPerfil(novaFoto);
            usuarioRepository.save(formUser);
        } else {
            // É a EDIÇÃO de um morador existente
            User existente = usuarioRepository.findById(formUser.getId()).orElse(null);
            if (existente != null) {
                existente.setNomeCompleto(formUser.getNomeCompleto());
                existente.setEmail(formUser.getEmail());
                existente.setUsername(formUser.getUsername());
                existente.setTelefone(formUser.getTelefone());
                if (formUser.getOrcamentoMensal() != null) {
                    existente.setOrcamentoMensal(formUser.getOrcamentoMensal());
                }
                existente.setAtivo(formUser.isAtivo());
                if (novaFoto != null) {
                    existente.setFotoPerfil(novaFoto);
                }

                // Só atualiza a senha se o usuário digitou uma nova no formulário
                if (formUser.getPassword() != null && !formUser.getPassword().trim().isEmpty()) {
                    existente.setPassword(passwordEncoder.encode(formUser.getPassword()));
                }
                usuarioRepository.save(existente);
            }
        }
        return "redirect:/app/settings/moradores";
    }

    @PostMapping("/remover/{id}")
    public String removerMorador(@PathVariable UUID id, java.security.Principal principal, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        log.info("Iniciando remoção do morador ID: {} por usuário: {}", id, principal.getName());
        User loggedUser = usuarioRepository.findByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("Registo não encontrado."));
        User toRemove = usuarioRepository.findById(id).orElse(null);

        if (toRemove == null) {
            log.warn("Morador com ID {} não encontrado para remoção.", id);
            redirectAttributes.addFlashAttribute("error", "Morador não encontrado.");
            return "redirect:/app/settings/moradores";
        }

        if (toRemove.getId().equals(loggedUser.getId())) {
            log.warn("Usuário {} tentou remover a si próprio.", principal.getName());
            redirectAttributes.addFlashAttribute("error", "Não pode remover a si próprio.");
            return "redirect:/app/settings/moradores";
        }

        // Para manter a integridade dos dados históricos, marcamos como inativo em vez de deletar
        log.info("Desativando morador: {} ({})", toRemove.getNomeCompleto(), toRemove.getEmail());
        toRemove.setAtivo(false);
        usuarioRepository.save(toRemove);

        log.info("Morador {} removido com sucesso.", toRemove.getEmail());
        redirectAttributes.addFlashAttribute("success", "Morador " + toRemove.getNomeCompleto() + " removido com sucesso.");
        return "redirect:/app/settings/moradores";
    }
}