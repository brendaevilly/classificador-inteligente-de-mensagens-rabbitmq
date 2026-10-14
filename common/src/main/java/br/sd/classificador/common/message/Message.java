package br.sd.classificador.common.message;

/**
 * Mensagem trafegada pelo sistema (serializada como JSON).
 *
 * @param id        identificador único (UUID)
 * @param timestamp data/hora de geração em ISO-8601 (ex.: 2026-10-12T22:40:01.123-03:00)
 * @param type      "review" (avaliação de produto) ou "support" (solicitação de suporte)
 * @param content   texto da mensagem
 */
public record Message(String id, String timestamp, String type, String content) {

    public static final String TYPE_REVIEW = "review";
    public static final String TYPE_SUPPORT = "support";
}
