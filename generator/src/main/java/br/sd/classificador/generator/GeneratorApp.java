package br.sd.classificador.generator;

import br.sd.classificador.common.Topology;
import br.sd.classificador.common.rabbit.RabbitConfig;
import br.sd.classificador.common.rabbit.RabbitConnector;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Container 1 - Gerador de Mensagens.
 * Publica continuamente avaliações de produtos e solicitações de suporte no Topic Exchange,
 * na taxa configurada (MESSAGES_PER_SECOND, padrão 10 msg/s).
 */
public final class GeneratorApp {

    private static final Logger log = LoggerFactory.getLogger(GeneratorApp.class);

    private final AtomicLong total = new AtomicLong();
    private final AtomicLong reviewsInWindow = new AtomicLong();
    private final AtomicLong supportInWindow = new AtomicLong();
    private final AtomicLong poisonTotal = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();

    public static void main(String[] args) throws Exception {
        new GeneratorApp().run();
    }

    private void run() throws Exception {
        GeneratorConfig config = GeneratorConfig.fromEnv();
        RabbitConfig rabbitConfig = RabbitConfig.fromEnv();
        log.info("Iniciando GERADOR | taxa={} msg/s | avaliações={}% | suporte={}% | poison a cada {} | broker={}",
                config.messagesPerSecond(), Math.round(config.reviewRatio() * 100),
                Math.round((1 - config.reviewRatio()) * 100),
                config.poisonEveryN() > 0 ? config.poisonEveryN() + " mensagens" : "(desligado)",
                rabbitConfig.describe());

        RabbitConnector connector = new RabbitConnector(rabbitConfig);
        Connection connection = connector.connect("generator");
        connector.awaitTopology(connection, List.of(Topology.EXCHANGE), List.of());

        MessagePublisher publisher = new MessagePublisher(connection);
        Random random = config.seed() != 0 ? new Random(config.seed()) : new Random();
        MessageFactory factory = new MessageFactory(config.reviewRatio(), config.poisonEveryN(), random);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        long periodMicros = Math.max(1, Math.round(1_000_000.0 / config.messagesPerSecond()));
        scheduler.scheduleAtFixedRate(() -> publishOne(factory, publisher, config.logEachMessage()),
                0, periodMicros, TimeUnit.MICROSECONDS);
        scheduler.scheduleAtFixedRate(this::logSummary, 1, 1, TimeUnit.SECONDS);

        CountDownLatch stopped = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Encerrando gerador...");
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            publisher.close();
            RabbitConnector.closeQuietly(connection);
            log.info("Gerador encerrado. Total publicado={} (venenosas={}, falhas={})",
                    total.get(), poisonTotal.get(), failures.get());
            stopped.countDown();
        }, "shutdown-hook"));
        stopped.await();
    }

    private void publishOne(MessageFactory factory, MessagePublisher publisher, boolean logEach) {
        OutgoingMessage message = factory.next();
        try {
            publisher.publish(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        } catch (Exception e) {
            // Ex.: conexão em recuperação automática. A tarefa continua agendada e volta a publicar sozinha.
            long count = failures.incrementAndGet();
            log.warn("Falha ao publicar id={} ({}): {} | falhas acumuladas={}",
                    message.id(), message.routingKey(), e.toString(), count);
            return;
        }
        long n = total.incrementAndGet();
        if (Topology.ROUTING_KEY_REVIEW.equals(message.routingKey())) {
            reviewsInWindow.incrementAndGet();
        } else {
            supportInWindow.incrementAndGet();
        }
        if (message.poison()) {
            poisonTotal.incrementAndGet();
            log.warn("[PUBLICADO VENENOSA] #{} {} | id={} | corpo={}", n, message.routingKey(), message.id(),
                    message.preview());
        } else if (logEach) {
            log.info("[PUBLICADO] #{} {} | id={} | \"{}\"", n, message.routingKey(), message.id(), message.preview());
        }
    }

    private void logSummary() {
        long reviews = reviewsInWindow.getAndSet(0);
        long support = supportInWindow.getAndSet(0);
        log.info("[RESUMO] {} msg/s no último segundo (review.product={}, support.request={}) | total={} | "
                        + "venenosas={} | falhas={}",
                reviews + support, reviews, support, total.get(), poisonTotal.get(), failures.get());
    }
}
