package br.sd.classificador.common.message;

/**
 * Indica que uma mensagem recebida não pode ser processada (JSON inválido, campo
 * obrigatório ausente, tipo incorreto, conteúdo venenoso...). Os consumidores
 * respondem a essa exceção com basicNack(requeue=false), enviando a mensagem à DLQ.
 */
public class InvalidMessageException extends Exception {

    private final String messageId;

    public InvalidMessageException(String reason, String messageId) {
        super(reason);
        this.messageId = messageId;
    }

    public InvalidMessageException(String reason, String messageId, Throwable cause) {
        super(reason, cause);
        this.messageId = messageId;
    }

    /** Id da mensagem, quando foi possível extraí-lo; caso contrário {@code null}. */
    public String getMessageId() {
        return messageId;
    }
}
