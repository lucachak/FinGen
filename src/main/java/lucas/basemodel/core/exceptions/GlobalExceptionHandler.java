package lucas.basemodel.core.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Sempre que a aplicação tentar rebentar com um erro de ficheiro grande,
    // este método "apanha" o erro no ar.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(MaxUploadSizeExceededException exc, RedirectAttributes redirectAttributes) {

        // Adiciona uma mensagem de erro invisível que será exibida no formulário
        redirectAttributes.addFlashAttribute("mensagemErro", "O ficheiro selecionado é demasiado grande! O limite de sistema é 10MB.");

        // Devolve o utilizador para a página de Nova Transação em segurança
        return "redirect:/contas/nova";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception exc, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("erro", "Ocorreu um erro interno: " + exc.getMessage());
        return "redirect:/app/dashboard";
    }
}