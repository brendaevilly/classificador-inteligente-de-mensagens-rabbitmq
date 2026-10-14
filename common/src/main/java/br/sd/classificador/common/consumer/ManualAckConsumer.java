package br.sd.classificador.common.consumer;

import br.sd.classificador.common.message.InvalidMessageException;
import br.sd.classificador.common.message.Message;
import br.sd.classificador.common.message.MessageCodec;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Consumidor com confirmação MANUAL (autoAck=false).
 * <ul>
 *   <li>Sucesso: basicAck somente depois de processar.</li>
 *   <li>Erro (JSON inválido, campo ausente, __POISON__, exceção na IA...):
 *       basicNack(requeue=false) -> o RabbitMQ encaminha a mensagem para o
 *       dead-letter exchange configurado na fila, que a entrega na DLQ (com header x-death).</li>
 * </ul>
 */
public class ManualAckConsumer extends DefaultConsumer {

    private static final Logger log = LoggerFactory.getLogger(ManualAckConsumer.class);

    private final ConsumerSettings settings;
    private final MessageProcessor processor;
    private final ReentrantLock processingLock = new ReentrantLock();
    private final AtomicLong acked = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private volatile boolean shuttingDown;

    public ManualAckConsumer(Channel channel, ConsumerSettings settings, MessageProcessor processor) {
        super(channel);
        this.settings = settings;
        this.processor = processor;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {
        long tag = envelope.getDeliveryTag();
        String amqpId = properties != null ? properties.getMessageId() : null;

        processingLock.lock();
        try {
            if (shuttingDown) {
                // Não confirma: ao fechar o canal a mensagem volta para a fila automaticamente.
                return;
            }

            Message message;
            try {
                message = MessageCodec.decodeAndValidate(body, settings.expectedType(), amqpId);
            } catch (InvalidMessageException e) {
                sendToDlq(tag, e.getMessageId(), e.getMessage(), body);
                return;
            }

            // Atraso artificial: simula processamento pesado para as filas crescerem no painel.
            if (!simulateWork()) {
                return;
            }

            String result;
            try {
                result = processor.process(message);
            } catch (Exception e) {
                sendToDlq(tag, message.id(), "falha no processamento/IA: " + e, body);
                return;
            }

            getChannel().basicAck(tag, false);
            long total = acked.incrementAndGet();
            log.info("ACK manual enviado | id={} | {} | processadas={} | rejeitadas(DLQ)={}",
                    message.id(), result, total, rejected.get());
        } finally {
            processingLock.unlock();
        }
    }

    private boolean simulateWork() {
        if (settings.processingDelayMs() <= 0) {
            return true;
        }
        try {
            Thread.sleep(settings.processingDelayMs());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Processamento interrompido (shutdown); a mensagem não confirmada voltará para a fila");
            return false;
        }
    }

    private void sendToDlq(long tag, String id, String reason, byte[] body) throws IOException {
        // requeue=false: a mensagem NÃO volta para a fila; vai para o dead-letter exchange.
        getChannel().basicNack(tag, false, false);
        long total = rejected.incrementAndGet();
        log.error("ERRO -> enviada para DLQ | fila={} | id={} | motivo={} | corpo={} | total rejeitadas={}",
                settings.queue(), id != null ? id : "desconhecido", reason, preview(body), total);
    }

    /**
     * Sinaliza o encerramento e espera a mensagem em processamento terminar (até o timeout).
     */
    public void prepareShutdown(long timeoutMs) {
        shuttingDown = true;
        try {
            if (processingLock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)) {
                processingLock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public long ackedCount() {
        return acked.get();
    }

    public long rejectedCount() {
        return rejected.get();
    }

    private static String preview(byte[] body) {
        if (body == null) {
            return "<vazio>";
        }
        String text = new String(body, java.nio.charset.StandardCharsets.UTF_8).replaceAll("\\s+", " ");
        return text.length() > 160 ? text.substring(0, 160) + "..." : text;
    }
}
