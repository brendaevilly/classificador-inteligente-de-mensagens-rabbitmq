package br.sd.classificador.generator;

import br.sd.classificador.common.Topology;
import br.sd.classificador.common.message.Message;
import br.sd.classificador.common.message.MessageCodec;
import br.sd.classificador.generator.templates.ReviewTemplates;
import br.sd.classificador.generator.templates.SupportTemplates;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.UUID;

/**
 * Cria as mensagens do sistema: avaliações (routing key review.product) e solicitações de
 * suporte (support.request), na proporção configurada, além de mensagens venenosas opcionais.
 */
public final class MessageFactory {

    private static final ZoneId ZONE = resolveZone();

    private final Random random;
    private final double reviewRatio;
    private final int poisonEveryN;
    private long sequence;
    private long poisonSequence;

    public MessageFactory(double reviewRatio, int poisonEveryN, Random random) {
        this.reviewRatio = reviewRatio;
        this.poisonEveryN = poisonEveryN;
        this.random = random;
    }

    /** Próxima mensagem do fluxo contínuo (pode ser venenosa se POISON_EVERY_N > 0). */
    public OutgoingMessage next() {
        sequence++;
        if (poisonEveryN > 0 && sequence % poisonEveryN == 0) {
            return poison();
        }
        return random.nextDouble() < reviewRatio ? review() : support();
    }

    public OutgoingMessage review() {
        ReviewTemplates.Review review = ReviewTemplates.generate(random);
        return build(Message.TYPE_REVIEW, Topology.ROUTING_KEY_REVIEW, review.text(), review.sentiment().name());
    }

    public OutgoingMessage support() {
        SupportTemplates.Request request = SupportTemplates.generate(random);
        return build(Message.TYPE_SUPPORT, Topology.ROUTING_KEY_SUPPORT, request.text(), request.category().name());
    }

    /**
     * Mensagem venenosa para demonstrar a DLQ. Alterna entre as duas filas e entre dois
     * tipos de erro: conteúdo "__POISON__" (JSON válido) e corpo que não é JSON.
     */
    public OutgoingMessage poison() {
        poisonSequence++;
        boolean toReview = poisonSequence % 2 == 1;
        boolean invalidJson = (poisonSequence / 2) % 2 == 1;
        String type = toReview ? Message.TYPE_REVIEW : Message.TYPE_SUPPORT;
        String routingKey = toReview ? Topology.ROUTING_KEY_REVIEW : Topology.ROUTING_KEY_SUPPORT;
        String id = UUID.randomUUID().toString();
        if (invalidJson) {
            byte[] body = ("{\"id\":\"" + id + "\",\"type\":\"" + type + "\",\"content\": isto nao e um JSON valido")
                    .getBytes(StandardCharsets.UTF_8);
            return new OutgoingMessage(id, routingKey, type, body, null, null, true);
        }
        Message message = new Message(id, now(), type, Topology.POISON_CONTENT);
        return new OutgoingMessage(id, routingKey, type, MessageCodec.encode(message), message, null, true);
    }

    private OutgoingMessage build(String type, String routingKey, String content, String expectedLabel) {
        Message message = new Message(UUID.randomUUID().toString(), now(), type, content);
        return new OutgoingMessage(message.id(), routingKey, type, MessageCodec.encode(message), message,
                expectedLabel, false);
    }

    /** Fuso da variável TZ (padrão America/Sao_Paulo); valores inválidos caem no fuso do sistema. */
    private static ZoneId resolveZone() {
        try {
            return ZoneId.of(System.getenv().getOrDefault("TZ", "America/Sao_Paulo"));
        } catch (RuntimeException e) {
            return ZoneId.systemDefault();
        }
    }

    private static String now() {
        return OffsetDateTime.now(ZONE).truncatedTo(ChronoUnit.MILLIS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
