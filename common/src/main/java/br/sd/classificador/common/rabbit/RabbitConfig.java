package br.sd.classificador.common.rabbit;

import br.sd.classificador.common.Env;

/**
 * Parâmetros de conexão com o RabbitMQ, lidos de variáveis de ambiente.
 *
 * @param host               RABBITMQ_HOST (padrão: rabbitmq, nome do serviço no compose)
 * @param port               RABBITMQ_PORT (padrão: 5672)
 * @param username           RABBITMQ_USER
 * @param password           RABBITMQ_PASS
 * @param virtualHost        RABBITMQ_VHOST (padrão: /)
 * @param maxConnectAttempts CONNECT_MAX_ATTEMPTS (0 = tentar indefinidamente)
 * @param initialBackoffMs   CONNECT_INITIAL_BACKOFF_MS (padrão: 1000)
 * @param maxBackoffMs       CONNECT_MAX_BACKOFF_MS (padrão: 15000)
 */
public record RabbitConfig(String host, int port, String username, String password, String virtualHost,
                           int maxConnectAttempts, long initialBackoffMs, long maxBackoffMs) {

    public static RabbitConfig fromEnv() {
        return new RabbitConfig(
                Env.get("RABBITMQ_HOST", "rabbitmq"),
                Env.getInt("RABBITMQ_PORT", 5672),
                Env.get("RABBITMQ_USER", "guest"),
                Env.get("RABBITMQ_PASS", "guest"),
                Env.get("RABBITMQ_VHOST", "/"),
                Env.getInt("CONNECT_MAX_ATTEMPTS", 0),
                Env.getLong("CONNECT_INITIAL_BACKOFF_MS", 1000),
                Env.getLong("CONNECT_MAX_BACKOFF_MS", 15000));
    }

    /** Representação segura para log (sem a senha). */
    public String describe() {
        return "amqp://" + username + "@" + host + ":" + port + virtualHost;
    }
}
