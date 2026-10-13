package lucas.basemodel.modules.financeiro.services;

import java.math.BigDecimal;

/**
 * Um modelo do catálogo do OpenRouter, reduzido ao que a interface precisa.
 *
 * @param id            id usado nas chamadas, ex.: {@code google/gemma-3-27b-it:free}
 * @param name          nome para exibição
 * @param contextLength janela de contexto em tokens, 0 se desconhecida
 * @param free          sem custo por token
 * @param vision        aceita imagem como entrada (recibos e extratos fotografados)
 * @param promptPrice   preço por token de entrada, nulo se desconhecido
 */
public record AiModelInfo(String id, String name, int contextLength, boolean free, boolean vision,
                          BigDecimal promptPrice) {
}
