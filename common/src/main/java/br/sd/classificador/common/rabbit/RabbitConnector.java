package br.sd.classificador.common.rabbit;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Cria conexões com o RabbitMQ com retry e backoff exponencial e verifica (de forma
 * passiva) que a topologia definida no definitions.json já está disponível.
 */
public final class RabbitConnector {

    private static final Logger log = LoggerFactory.getLogger(RabbitConnector.class);

    private final RabbitConfig config;

    public RabbitConnector(RabbitConfig config) {
        this.config = config;
    }

    /**
     * Abre a conexão, tentando novamente com backoff exponencial enquanto o broker não
     * estiver pronto. Depois de conectada, a recuperação automática do amqp-client
     * reconecta sozinha em caso de queda (reabrindo canais, QoS e consumidores).
     */
    public Connection connect(String connectionName) throws InterruptedException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.host());
        factory.setPort(config.port());
        factory.setUsername(config.username());
        factory.setPassword(config.password());
        factory.setVirtualHost(config.virtualHost());
        factory.setAutomaticRecoveryEnabled(true);
        factory.setTopologyRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(5000);
        factory.setRequestedHeartbeat(30);
        factory.setConnectionTimeout(10_000);

        long backoff = config.initialBackoffMs();
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                Connection connection = factory.newConnection(connectionName);
                log.info("Conectado ao RabbitMQ em {} (tentativa {})", config.describe(), attempt);
                return connection;
            } catch (IOException | TimeoutException e) {
                if (config.maxConnectAttempts() > 0 && attempt >= config.maxConnectAttempts()) {
                    throw new IllegalStateException("Não foi possível conectar ao RabbitMQ após " + attempt
                            + " tentativas", e);
                }
                log.warn("RabbitMQ indisponível em {} (tentativa {}): {}. Nova tentativa em {} ms",
                        config.describe(), attempt, rootMessage(e), backoff);
                Thread.sleep(backoff);
                backoff = Math.min(backoff * 2, config.maxBackoffMs());
            }
        }
    }

    /**
     * Aguarda até que exchanges e filas existam no broker usando declarações PASSIVAS
     * (que nunca criam nem alteram nada). Cada falha passiva fecha o canal, por isso um
     * canal novo é aberto a cada verificação.
     */
    public void awaitTopology(Connection connection, List<String> exchanges, List<String> queues)
            throws InterruptedException {
        long backoff = config.initialBackoffMs();
        while (true) {
            String missing = null;
            try {
                for (String exchange : exchanges) {
                    missing = "exchange '" + exchange + "'";
                    try (Channel ch = connection.createChannel()) {
                        ch.exchangeDeclarePassive(exchange);
                    }
                }
                for (String queue : queues) {
                    missing = "fila '" + queue + "'";
                    try (Channel ch = connection.createChannel()) {
                        ch.queueDeclarePassive(queue);
                    }
                }
                log.info("Topologia verificada: exchanges={} filas={}", exchanges, queues);
                return;
            } catch (IOException | TimeoutException | RuntimeException e) {
                log.warn("Topologia ainda não disponível ({} não encontrado(a)): {}. Nova verificação em {} ms",
                        missing, rootMessage(e), backoff);
                Thread.sleep(backoff);
                backoff = Math.min(backoff * 2, config.maxBackoffMs());
            }
        }
    }

    /** Fecha conexão ignorando erros (usado no shutdown). */
    public static void closeQuietly(Connection connection) {
        if (connection != null && connection.isOpen()) {
            try {
                connection.close(5000);
            } catch (Exception e) {
                log.debug("Erro ao fechar conexão: {}", e.getMessage());
            }
        }
    }

    /** Fecha canal ignorando erros (usado no shutdown). */
    public static void closeQuietly(Channel channel) {
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
            } catch (Exception e) {
                log.debug("Erro ao fechar canal: {}", e.getMessage());
            }
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String msg = t.getMessage();
        return t.getClass().getSimpleName() + (msg != null ? " - " + msg : "");
    }
}
