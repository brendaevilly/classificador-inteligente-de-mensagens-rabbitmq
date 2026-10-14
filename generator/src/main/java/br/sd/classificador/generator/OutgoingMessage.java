package br.sd.classificador.generator;

import br.sd.classificador.common.message.Message;

/**
 * Mensagem pronta para publicação.
 *
 * @param id            id da mensagem (também enviado na propriedade AMQP message_id)
 * @param routingKey    review.product ou support.request
 * @param type          review ou support
 * @param body          bytes publicados (normalmente o JSON da {@link Message})
 * @param message       mensagem estruturada; {@code null} quando o corpo é propositalmente um JSON inválido
 * @param expectedLabel rótulo "verdadeiro" do template usado. NÃO é publicado: serve apenas para
 *                      medir a acurácia das IAs nos testes (módulo evaluation)
 * @param poison        {@code true} se é uma mensagem venenosa (demonstração de DLQ)
 */
public record OutgoingMessage(String id, String routingKey, String type, byte[] body, Message message,
                              String expectedLabel, boolean poison) {

    /** Texto curto para log. */
    public String preview() {
        String text = message != null ? message.content() : new String(body, java.nio.charset.StandardCharsets.UTF_8);
        return text.length() > 110 ? text.substring(0, 110) + "..." : text;
    }
}
