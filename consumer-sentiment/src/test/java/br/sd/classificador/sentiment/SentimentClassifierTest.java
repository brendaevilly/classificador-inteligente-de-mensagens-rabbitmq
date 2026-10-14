package br.sd.classificador.sentiment;

import br.sd.classificador.common.nlp.ModelEvaluation;
import br.sd.classificador.common.nlp.Prediction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SentimentClassifierTest {

    private static SentimentClassifier classifier;

    @BeforeAll
    static void train() {
        classifier = SentimentClassifier.train();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "POSITIVA | Adorei, o produto é excelente e chegou rápido!",
            "POSITIVA | Muito bom, recomendo para todo mundo.",
            "POSITIVA | Qualidade incrível, superou minhas expectativas.",
            "NEGATIVA | Péssimo, quebrou no primeiro dia e ninguém resolve.",
            "NEGATIVA | Horrível, não recomendo, dinheiro jogado fora.",
            "NEGATIVA | Veio com defeito e me arrependi da compra.",
            "NEUTRA   | É um produto razoável, cumpre o básico.",
            "NEUTRA   | Normal, nada de especial, nem bom nem ruim.",
            "NEUTRA   | Mediano, atende ao que promete e só."
    })
    void classifiesClearExamples(String expected, String text) {
        assertEquals(expected, classifier.classify(text), text);
    }

    @Test
    void returnsProbabilitiesThatSumToOne() {
        Prediction<Sentiment> p = classifier.predict("Produto ótimo, recomendo!");
        double sum = p.probabilities().values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, sum, 1e-6);
        assertTrue(p.confidence() > 0.5);
    }

    @Test
    void datasetHasEnoughExamplesPerClass() {
        classifier.dataset().countByLabel().forEach((label, count) ->
                assertTrue(count >= 60, label + " tem só " + count + " exemplos"));
    }

    @Test
    void crossValidationAccuracyIsReasonable() {
        ModelEvaluation evaluation = classifier.evaluate(5);
        System.out.println("Sentimento - acurácia 5-fold: " + evaluation.accuracyPercent());
        assertTrue(evaluation.accuracy() >= 0.75, "acurácia: " + evaluation.accuracy());
    }
}
