package br.sd.classificador.common.message;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageCodecTest {

    private static final String NOW = "2026-10-12T22:40:01.123-03:00";

    @Test
    void roundTripPreservesAllFields() throws Exception {
        Message original = new Message(UUID.randomUUID().toString(), NOW, "review", "Produto ótimo!");
        Message decoded = MessageCodec.decodeAndValidate(MessageCodec.encode(original), "review", null);
        assertEquals(original, decoded);
    }

    @Test
    void rejectsInvalidJson() {
        InvalidMessageException e = assertThrows(InvalidMessageException.class,
                () -> MessageCodec.decodeAndValidate(bytes("{ isto nao e json"), "review", "id-amqp"));
        assertTrue(e.getMessage().startsWith("JSON inválido"));
        assertEquals("id-amqp", e.getMessageId());
    }

    @Test
    void rejectsMissingField() {
        String json = "{\"id\":\"" + UUID.randomUUID() + "\",\"timestamp\":\"" + NOW + "\",\"type\":\"review\"}";
        InvalidMessageException e = assertThrows(InvalidMessageException.class,
                () -> MessageCodec.decodeAndValidate(bytes(json), "review", null));
        assertTrue(e.getMessage().contains("content"));
    }

    @Test
    void rejectsPoisonContent() {
        Message poison = new Message(UUID.randomUUID().toString(), NOW, "support", "__POISON__");
        InvalidMessageException e = assertThrows(InvalidMessageException.class,
                () -> MessageCodec.decodeAndValidate(MessageCodec.encode(poison), "support", null));
        assertTrue(e.getMessage().contains("venenosa"));
        assertEquals(poison.id(), e.getMessageId());
    }

    @Test
    void rejectsWrongType() {
        Message support = new Message(UUID.randomUUID().toString(), NOW, "support", "Sem internet");
        assertThrows(InvalidMessageException.class,
                () -> MessageCodec.decodeAndValidate(MessageCodec.encode(support), "review", null));
    }

    @Test
    void rejectsInvalidTimestampAndId() {
        assertThrows(InvalidMessageException.class, () -> MessageCodec.decodeAndValidate(
                MessageCodec.encode(new Message(UUID.randomUUID().toString(), "ontem", "review", "ok")), "review", null));
        assertThrows(InvalidMessageException.class, () -> MessageCodec.decodeAndValidate(
                MessageCodec.encode(new Message("123", NOW, "review", "ok")), "review", null));
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
