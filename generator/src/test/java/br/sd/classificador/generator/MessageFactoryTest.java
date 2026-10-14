package br.sd.classificador.generator;

import br.sd.classificador.common.Topology;
import br.sd.classificador.common.message.InvalidMessageException;
import br.sd.classificador.common.message.Message;
import br.sd.classificador.common.message.MessageCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageFactoryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void messagesHaveAllRequiredJsonFields() throws Exception {
        MessageFactory factory = new MessageFactory(0.5, 0, new Random(1));
        for (int i = 0; i < 200; i++) {
            OutgoingMessage out = factory.next();
            JsonNode json = MAPPER.readTree(out.body());

            assertEquals(4, json.size(), "JSON deve ter exatamente id, timestamp, type e content");
            assertDoesNotThrow(() -> UUID.fromString(json.get("id").asText()));
            assertDoesNotThrow(() -> OffsetDateTime.parse(json.get("timestamp").asText()));
            assertFalse(json.get("content").asText().isBlank());
            assertEquals(out.id(), json.get("id").asText());

            String type = json.get("type").asText();
            if (type.equals(Message.TYPE_REVIEW)) {
                assertEquals(Topology.ROUTING_KEY_REVIEW, out.routingKey());
            } else {
                assertEquals(Message.TYPE_SUPPORT, type);
                assertEquals(Topology.ROUTING_KEY_SUPPORT, out.routingKey());
            }
            assertNotNull(out.expectedLabel());
            assertDoesNotThrow(() -> MessageCodec.decodeAndValidate(out.body(), type, null));
        }
    }

    @Test
    void idsAreUnique() {
        MessageFactory factory = new MessageFactory(0.5, 0, new Random(2));
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            assertTrue(ids.add(factory.next().id()));
        }
    }

    @Test
    void distributionFollowsReviewRatio() {
        MessageFactory factory = new MessageFactory(0.5, 0, new Random(3));
        int reviews = 0;
        int total = 10_000;
        for (int i = 0; i < total; i++) {
            if (factory.next().type().equals(Message.TYPE_REVIEW)) {
                reviews++;
            }
        }
        double ratio = (double) reviews / total;
        assertTrue(ratio > 0.47 && ratio < 0.53, "proporção de avaliações: " + ratio);
    }

    @Test
    void producesVariedTexts() {
        MessageFactory factory = new MessageFactory(0.5, 0, new Random(4));
        Set<String> texts = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            texts.add(factory.next().message().content());
        }
        assertTrue(texts.size() > 900, "textos distintos: " + texts.size());
    }

    @Test
    void poisonEveryNIsRejectedByConsumersValidation() {
        MessageFactory factory = new MessageFactory(0.5, 5, new Random(5));
        int poison = 0;
        for (int i = 1; i <= 40; i++) {
            OutgoingMessage out = factory.next();
            if (i % 5 == 0) {
                assertTrue(out.poison());
                poison++;
                assertThrows(InvalidMessageException.class,
                        () -> MessageCodec.decodeAndValidate(out.body(), out.type(), out.id()));
            } else {
                assertFalse(out.poison());
            }
        }
        assertEquals(8, poison);
    }
}
