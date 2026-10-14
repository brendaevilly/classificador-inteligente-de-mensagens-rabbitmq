package br.sd.classificador.common.consumer;

import br.sd.classificador.common.Topology;
import br.sd.classificador.common.rabbit.RabbitConfig;
import br.sd.classificador.common.rabbit.RabbitConnector;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Ciclo de vida de um consumidor: conecta (com retry), verifica a topologia, configura
 * o QoS (prefetch), começa a consumir com autoAck=false e trata o shutdown gracioso.
 */
public final class ConsumerRunner {

    private static final Logger log = LoggerFactory.getLogger(ConsumerRunner.class);

    private final ConsumerSettings settings;
    private final MessageProcessor processor;
    private final RabbitConfig rabbitConfig;
    private final CountDownLatch stopped = new CountDownLatch(1);

    public ConsumerRunner(ConsumerSettings settings, MessageProcessor processor, RabbitConfig rabbitConfig) {
        this.settings = settings;
        this.processor = processor;
        this.rabbitConfig = rabbitConfig;
    }

    /** Bloqueia até o processo receber sinal de término (SIGTERM / docker stop). */
    public void runUntilShutdown() throws Exception {
        RabbitConnector connector = new RabbitConnector(rabbitConfig);
        Connection connection = connector.connect(settings.serviceName());
        connector.awaitTopology(connection,
                List.of(Topology.EXCHANGE, Topology.DEAD_LETTER_EXCHANGE),
                List.of(settings.queue()));

        Channel channel = connection.createChannel();
        // prefetch pequeno: as mensagens ficam "Ready" na fila (visível no painel) e não no buffer do cliente.
        channel.basicQos(settings.prefetch());

        ManualAckConsumer consumer = new ManualAckConsumer(channel, settings, processor);
        boolean autoAck = false;
        String consumerTag = channel.basicConsume(settings.queue(), autoAck, settings.serviceName(), consumer);

        log.info("Consumindo a fila '{}' | autoAck={} | prefetch={} | atraso artificial={} ms | consumerTag={}",
                settings.queue(), autoAck, settings.prefetch(), settings.processingDelayMs(), consumerTag);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Encerrando {}: cancelando consumo e aguardando mensagem em andamento...", settings.serviceName());
            try {
                if (channel.isOpen()) {
                    channel.basicCancel(consumerTag);
                }
            } catch (Exception e) {
                log.debug("Falha ao cancelar consumidor: {}", e.getMessage());
            }
            consumer.prepareShutdown(settings.processingDelayMs() + 5000);
            RabbitConnector.closeQuietly(channel);
            RabbitConnector.closeQuietly(connection);
            log.info("{} encerrado. Confirmadas (ACK)={} | Rejeitadas (DLQ)={}",
                    settings.serviceName(), consumer.ackedCount(), consumer.rejectedCount());
            stopped.countDown();
        }, "shutdown-hook"));

        stopped.await();
    }
}
