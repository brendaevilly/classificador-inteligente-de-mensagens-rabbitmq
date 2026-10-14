package br.sd.classificador.sentiment;

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
 * Container 3 - Consumidor 1: consome SOMENTE a fila review.queue e classifica o
 * sentimento de cada avaliação com a IA embarcada ({@link SentimentClassifier}).
 */
public final class SentimentConsumerApp {

    private static final Logger log = LoggerFactory.getLogger(SentimentConsumerApp.class);

    public static void main(String[] args) throws Exception {
        log.info("Iniciando CONSUMIDOR 1 (análise de sentimento) - treinando a IA embarcada...");
        long start = System.currentTimeMillis();
        SentimentClassifier classifier = SentimentClassifier.train();
        long trainMs = System.currentTimeMillis() - start;
        ModelEvaluation evaluation = classifier.evaluate(5);

        log.info("IA de sentimento pronta | algoritmo=Smile DiscreteNaiveBayes (MULTINOMIAL) | dataset={} exemplos {} "
                        + "| vocabulário={} termos | treino em {} ms",
                classifier.dataset().size(), classifier.dataset().countByLabel(), classifier.vocabularySize(), trainMs);
        log.info("Acurácia estimada (validação cruzada estratificada {}-fold): {}{}{}",
                evaluation.folds(), evaluation.accuracyPercent(), System.lineSeparator(),
                evaluation.formatConfusion(Sentiment.class));

        ConsumerSettings settings = ConsumerSettings.fromEnv("consumer-sentiment", Topology.REVIEW_QUEUE,
                Message.TYPE_REVIEW);
        new ConsumerRunner(settings, processor(classifier), RabbitConfig.fromEnv()).runUntilShutdown();
    }

    static MessageProcessor processor(SentimentClassifier classifier) {
        return message -> {
            Prediction<Sentiment> prediction = classifier.predict(message.content());
            log.info("[IA-SENTIMENTO] id={} | classificação={} | confiança={} | texto=\"{}\"",
                    message.id(), prediction.label(), prediction.confidencePercent(), message.content());
            return "sentimento=" + prediction.label();
        };
    }
}
