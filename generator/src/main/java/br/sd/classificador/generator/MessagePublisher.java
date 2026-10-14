package br.sd.classificador.generator;

import br.sd.classificador.common.Topology;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeoutException;

/**
 * Publica mensagens no Topic Exchange como mensagens persistentes (deliveryMode=2),
 * content-type application/json, com publisher confirms e flag mandatory (mensagens sem
 * rota são devolvidas e logadas, em vez de descartadas silenciosamente).
 */
public final class MessagePublisher implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MessagePublisher.class);
    private static final int PERSISTENT = 2;
    private static final long CONFIRM_TIMEOUT_MS = 5000;

    private final Channel channel;

    public MessagePublisher(Connection connection) throws IOException {
        this.channel = connection.createChannel();
        this.channel.confirmSelect();
        this.channel.addReturnListener(returned -> log.warn(
                "Mensagem SEM ROTA devolvida pelo broker: routingKey={} replyText={} corpo={}",
                returned.getRoutingKey(), returned.getReplyText(),
                new String(returned.getBody(), StandardCharsets.UTF_8)));
    }

    public void publish(OutgoingMessage message) throws IOException, InterruptedException, TimeoutException {
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("UTF-8")
                .deliveryMode(PERSISTENT)
                .messageId(message.id())
                .type(message.type())
                .timestamp(new Date())
                .appId("generator")
                .build();
        boolean mandatory = true;
        channel.basicPublish(Topology.EXCHANGE, message.routingKey(), mandatory, properties, message.body());
        // Aguarda a confirmação do broker. waitForConfirms (e não ...OrDie) para que um nack
        // isolado não feche o canal do publicador.
        if (!channel.waitForConfirms(CONFIRM_TIMEOUT_MS)) {
            throw new IOException("broker respondeu NACK para a mensagem " + message.id());
        }
    }

    @Override
    public void close() {
        if (channel.isOpen()) {
            try {
                channel.close();
            } catch (Exception e) {
                log.debug("Erro ao fechar canal do publicador: {}", e.getMessage());
            }
        }
    }
}
