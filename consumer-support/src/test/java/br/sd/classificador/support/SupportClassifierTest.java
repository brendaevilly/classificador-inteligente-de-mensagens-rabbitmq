package br.sd.classificador.support;

import br.sd.classificador.common.nlp.ModelEvaluation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportClassifierTest {

    private static SupportClassifier classifier;

    @BeforeAll
    static void train() {
        classifier = SupportClassifier.train();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "PROBLEMA DE ACESSO     | Não consigo fazer login, minha senha é recusada.",
            "PROBLEMA DE ACESSO     | Minha conta foi bloqueada e aparece acesso negado.",
            "PROBLEMA DE ACESSO     | Não recebo o código de verificação para entrar.",
            "PROBLEMA DE DESEMPENHO | O sistema está muito lento e trava ao abrir relatórios.",
            "PROBLEMA DE DESEMPENHO | A página demora muito para carregar.",
            "PROBLEMA DE DESEMPENHO | O computador congela e o uso de memória é altíssimo.",
            "PROBLEMA DE CONEXÃO    | Estou sem internet, o Wi-Fi caiu.",
            "PROBLEMA DE CONEXÃO    | A VPN não conecta e dá timeout no servidor.",
            "PROBLEMA DE CONEXÃO    | A rede está instável e a conexão cai toda hora."
    })
    void classifiesClearExamples(String expected, String text) {
        assertEquals(expected, classifier.classify(text), text);
    }

    @Test
    void datasetHasEnoughExamplesPerClass() {
        classifier.dataset().countByLabel().forEach((label, count) ->
                assertTrue(count >= 60, label + " tem só " + count + " exemplos"));
    }

    @Test
    void crossValidationAccuracyIsReasonable() {
        ModelEvaluation evaluation = classifier.evaluate(5);
        System.out.println("Suporte - acurácia 5-fold: " + evaluation.accuracyPercent());
        assertTrue(evaluation.accuracy() >= 0.80, "acurácia: " + evaluation.accuracy());
    }
}
