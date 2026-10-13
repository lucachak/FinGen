package lucas.basemodel.modules.financeiro.services;

/**
 * Modelos a usar nas chamadas de um usuário.
 *
 * @param text   modelo para conversa, análises e categorização
 * @param vision modelo para ler imagens; é o mesmo de {@code text} quando ele aceita imagem
 */
public record AiModelSelection(String text, String vision) {
}
