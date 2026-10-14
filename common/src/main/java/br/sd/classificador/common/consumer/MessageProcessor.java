package br.sd.classificador.common.consumer;

import br.sd.classificador.common.message.Message;

/**
 * Lógica de negócio de um consumidor: recebe uma mensagem já validada e a processa
 * (no nosso caso, classifica com a IA e registra o resultado no log).
 * Qualquer exceção lançada aqui faz a mensagem ser rejeitada e enviada à DLQ.
 */
@FunctionalInterface
public interface MessageProcessor {

    /**
     * @return descrição curta do resultado (ex.: "POSITIVA (97,1%)"), usada no log
     */
    String process(Message message) throws Exception;
}
