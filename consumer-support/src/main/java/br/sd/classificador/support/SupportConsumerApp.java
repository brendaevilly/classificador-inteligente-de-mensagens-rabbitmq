package br.sd.classificador.support;

import br.sd.classificador.common.Topology;
import br.sd.classificador.common.consumer.ConsumerRunner;
import br.sd.classificador.common.consumer.ConsumerSettings;
import br.sd.classificador.common.consumer.MessageProcessor;
import br.sd.classificador.common.message.Message;
import br.sd.classificador.common.nlp.ModelEvaluation;
import br.sd.classificador.common.nlp.Prediction;
import br.sd.classificador.common.rabbit.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container 4 - Consumidor 2: consome SOMENTE a fila support.queue e classifica cada
 * solicitação com a IA embarcada ({@link SupportClassifier}).
 */
public final class SupportConsumerApp {

    private static final Logger log = LoggerFactory.getLogger(SupportConsumerApp.class);

    public static void main(String[] args) throws Exception {
        log.info("Iniciando CONSUMIDOR 2 (classificação de suporte) - treinando a IA embarcada...");
        long start = System.currentTimeMillis();
        SupportClassifier classifier = SupportClassifier.train();
        long trainMs = System.currentTimeMillis() - start;
        ModelEvaluation evaluation = classifier.evaluate(5);

        log.info("IA de suporte pronta | algoritmo=Smile DiscreteNaiveBayes (MULTINOMIAL) | dataset={} exemplos {} "
                        + "| vocabulário={} termos | treino em {} ms",
                classifier.dataset().size(), classifier.dataset().countByLabel(), classifier.vocabularySize(), trainMs);
        log.info("Acurácia estimada (validação cruzada estratificada {}-fold): {}{}{}",
                evaluation.folds(), evaluation.accuracyPercent(), System.lineSeparator(),
                evaluation.formatConfusion(SupportCategory.class));

        ConsumerSettings settings = ConsumerSettings.fromEnv("consumer-support", Topology.SUPPORT_QUEUE,
                Message.TYPE_SUPPORT);
        new ConsumerRunner(settings, processor(classifier), RabbitConfig.fromEnv()).runUntilShutdown();
    }

    static MessageProcessor processor(SupportClassifier classifier) {
        return message -> {
            Prediction<SupportCategory> prediction = classifier.predict(message.content());
            log.info("[IA-SUPORTE] id={} | categoria={} | confiança={} | texto=\"{}\"",
                    message.id(), prediction.label().displayName(), prediction.confidencePercent(), message.content());
            return "categoria=" + prediction.label().displayName();
        };
    }
}
