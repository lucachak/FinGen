package lucas.basemodel.core.exceptions;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
@lombok.extern.slf4j.Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(MaxUploadSizeExceededException exc, RedirectAttributes redirectAttributes) {
        log.warn("Upload size exceeded: {}", exc.getMessage());
        redirectAttributes.addFlashAttribute("mensagemErro",
                "O ficheiro selecionado é demasiado grande! O limite de sistema é 10MB.");
        return "redirect:/contas/nova";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception exc, RedirectAttributes redirectAttributes) {
        log.error("Unhandled exception: ", exc);
        redirectAttributes.addFlashAttribute("erro", "Ocorreu um erro interno. Por favor, tente novamente mais tarde.");
        return "redirect:/app/dashboard";
    }
}