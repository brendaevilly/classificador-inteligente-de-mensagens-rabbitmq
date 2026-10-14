package br.sd.classificador.common.consumer;

import br.sd.classificador.common.Env;

/**
 * Configuração de um consumidor.
 *
 * @param serviceName       nome lógico do serviço (aparece nos logs e no nome da conexão)
 * @param queue             fila consumida
 * @param expectedType      tipo de mensagem aceito pela fila ("review" ou "support")
 * @param prefetch          PREFETCH_COUNT: máximo de mensagens não confirmadas no consumidor
 * @param processingDelayMs PROCESSING_DELAY_MS: atraso artificial por mensagem
 */
public record ConsumerSettings(String serviceName, String queue, String expectedType,
                               int prefetch, long processingDelayMs) {

    public static ConsumerSettings fromEnv(String serviceName, String queue, String expectedType) {
        int prefetch = Env.getInt("PREFETCH_COUNT", 1);
        long delay = Env.getLong("PROCESSING_DELAY_MS", 1000);
        if (prefetch < 1) {
            throw new IllegalArgumentException("PREFETCH_COUNT deve ser >= 1");
        }
        if (delay < 0) {
            throw new IllegalArgumentException("PROCESSING_DELAY_MS deve ser >= 0");
        }
        return new ConsumerSettings(serviceName, queue, expectedType, prefetch, delay);
    }
}
