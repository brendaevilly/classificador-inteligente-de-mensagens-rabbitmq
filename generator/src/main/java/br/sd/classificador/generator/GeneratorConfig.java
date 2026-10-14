package br.sd.classificador.generator;

import br.sd.classificador.common.Env;

/**
 * Configuração do gerador (variáveis de ambiente).
 *
 * @param messagesPerSecond MESSAGES_PER_SECOND (padrão 10; mínimo exigido 5)
 * @param reviewRatio       REVIEW_RATIO: fração de avaliações (padrão 0.5 = 50% / 50%)
 * @param poisonEveryN      POISON_EVERY_N: a cada N mensagens envia uma venenosa (0 = desligado)
 * @param logEachMessage    LOG_EACH_MESSAGE: loga cada mensagem publicada (além do resumo por segundo)
 * @param seed              GENERATOR_SEED: semente do gerador aleatório (0 = aleatória)
 */
public record GeneratorConfig(double messagesPerSecond, double reviewRatio, int poisonEveryN,
                              boolean logEachMessage, long seed) {

    public static GeneratorConfig fromEnv() {
        GeneratorConfig config = new GeneratorConfig(
                Env.getDouble("MESSAGES_PER_SECOND", 10),
                Env.getDouble("REVIEW_RATIO", 0.5),
                Env.getInt("POISON_EVERY_N", 0),
                Env.getBoolean("LOG_EACH_MESSAGE", true),
                Env.getLong("GENERATOR_SEED", 0));
        config.validate();
        return config;
    }

    void validate() {
        if (messagesPerSecond <= 0 || messagesPerSecond > 1000) {
            throw new IllegalArgumentException("MESSAGES_PER_SECOND deve estar entre 0 e 1000");
        }
        if (reviewRatio < 0 || reviewRatio > 1) {
            throw new IllegalArgumentException("REVIEW_RATIO deve estar entre 0 e 1");
        }
        if (poisonEveryN < 0) {
            throw new IllegalArgumentException("POISON_EVERY_N deve ser >= 0");
        }
    }
}
