package br.sd.classificador.common;

/**
 * Nomes da topologia do RabbitMQ usados pelos serviços.
 * <p>
 * A fonte de verdade da topologia é o arquivo {@code rabbitmq/definitions.json},
 * carregado pelo próprio RabbitMQ na inicialização. Os serviços Java NÃO declaram
 * exchanges/filas: apenas verificam (declaração passiva) que elas existem. Assim é
 * impossível ocorrer PRECONDITION_FAILED por argumentos divergentes entre serviços.
 * Se algum nome mudar aqui, ele deve ser alterado também no definitions.json.
 */
public final class Topology {

    /** Topic exchange principal, onde o gerador publica todas as mensagens. */
    public static final String EXCHANGE = "messages.exchange";

    /** Routing key das avaliações de produtos. */
    public static final String ROUTING_KEY_REVIEW = "review.product";

    /** Routing key das solicitações de suporte. */
    public static final String ROUTING_KEY_SUPPORT = "support.request";

    /** Fila do Consumidor 1 (análise de sentimento). */
    public static final String REVIEW_QUEUE = "review.queue";

    /** Fila do Consumidor 2 (classificação de suporte). */
    public static final String SUPPORT_QUEUE = "support.queue";

    /** Dead Letter Exchange (direct) para onde vão as mensagens rejeitadas. */
    public static final String DEAD_LETTER_EXCHANGE = "dlx.exchange";

    /** Dead Letter Queue das avaliações. */
    public static final String REVIEW_DLQ = "review.dlq";

    /** Dead Letter Queue das solicitações de suporte. */
    public static final String SUPPORT_DLQ = "support.dlq";

    /** Conteúdo especial que os consumidores tratam como mensagem venenosa. */
    public static final String POISON_CONTENT = "__POISON__";

    private Topology() {
    }
}
