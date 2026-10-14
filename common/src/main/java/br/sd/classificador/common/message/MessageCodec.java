package br.sd.classificador.common.message;

import br.sd.classificador.common.Topology;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Serialização/desserialização JSON das mensagens com validação dos campos obrigatórios.
 */
public final class MessageCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private MessageCodec() {
    }

    public static byte[] encode(Message message) {
        try {
            return MAPPER.writeValueAsBytes(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar mensagem " + message.id(), e);
        }
    }

    public static String encodeToString(Message message) {
        return new String(encode(message), StandardCharsets.UTF_8);
    }

    /**
     * Converte o corpo recebido em {@link Message} e valida todos os campos.
     *
     * @param body         bytes recebidos do RabbitMQ
     * @param expectedType tipo que a fila deveria conter ("review" ou "support")
     * @param fallbackId   id vindo das propriedades AMQP (messageId), usado no log se o JSON for inválido
     * @throws InvalidMessageException se a mensagem não puder ser processada
     */
    public static Message decodeAndValidate(byte[] body, String expectedType, String fallbackId)
            throws InvalidMessageException {
        if (body == null || body.length == 0) {
            throw new InvalidMessageException("corpo da mensagem vazio", fallbackId);
        }

        JsonNode root;
        try {
            root = MAPPER.readTree(body);
        } catch (IOException e) {
            throw new InvalidMessageException("JSON inválido: " + firstLine(e.getMessage()), fallbackId, e);
        }
        if (root == null || !root.isObject()) {
            throw new InvalidMessageException("JSON inválido: esperado um objeto", fallbackId);
        }

        String id = text(root, "id");
        String reportedId = id != null ? id : fallbackId;
        String timestamp = text(root, "timestamp");
        String type = text(root, "type");
        String content = text(root, "content");

        requireField(id, "id", reportedId);
        requireField(timestamp, "timestamp", reportedId);
        requireField(type, "type", reportedId);
        requireField(content, "content", reportedId);

        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new InvalidMessageException("campo 'id' não é um UUID válido: " + id, reportedId);
        }
        try {
            OffsetDateTime.parse(timestamp);
        } catch (DateTimeParseException e) {
            throw new InvalidMessageException("campo 'timestamp' não está em ISO-8601: " + timestamp, reportedId);
        }
        if (expectedType != null && !expectedType.equals(type)) {
            throw new InvalidMessageException(
                    "tipo inesperado '" + type + "' (esta fila só aceita '" + expectedType + "')", reportedId);
        }
        if (Topology.POISON_CONTENT.equals(content.trim())) {
            throw new InvalidMessageException("mensagem venenosa detectada (content = " + Topology.POISON_CONTENT + ")",
                    reportedId);
        }

        return new Message(id, timestamp, type, content);
    }

    private static void requireField(String value, String field, String reportedId) throws InvalidMessageException {
        if (value == null || value.isBlank()) {
            throw new InvalidMessageException("campo obrigatório ausente ou vazio: '" + field + "'", reportedId);
        }
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return (node == null || node.isNull()) ? null : node.asText();
    }

    private static String firstLine(String text) {
        if (text == null) {
            return "";
        }
        int idx = text.indexOf('\n');
        return idx >= 0 ? text.substring(0, idx) : text;
    }
}
