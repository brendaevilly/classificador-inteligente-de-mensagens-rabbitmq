package br.sd.classificador.evaluation;

import br.sd.classificador.generator.templates.ReviewTemplates;
import br.sd.classificador.generator.templates.SupportTemplates;
import br.sd.classificador.sentiment.Sentiment;
import br.sd.classificador.sentiment.SentimentClassifier;
import br.sd.classificador.support.SupportCategory;
import br.sd.classificador.support.SupportClassifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mede a acurácia das IAs sobre mensagens geradas pelo próprio gerador (rótulo verdadeiro =
 * classe do template usado). O relatório é impresso e salvo em target/accuracy-report.txt.
 */
class GeneratedMessagesAccuracyTest {

    private static final int SAMPLES_PER_CLASS = 1000;
    private static final double MIN_ACCURACY = 0.90;
    private static final Path REPORT = Path.of("target", "accuracy-report.txt");

    @Test
    void sentimentClassifierHandlesGeneratedReviews() throws IOException {
        SentimentClassifier classifier = SentimentClassifier.train();
        Random random = new Random(2026);
        Sentiment[] labels = Sentiment.values();
        int[][] confusion = new int[labels.length][labels.length];
        List<String> errors = new ArrayList<>();

        for (ReviewTemplates.Sentiment expected : ReviewTemplates.Sentiment.values()) {
            for (int i = 0; i < SAMPLES_PER_CLASS; i++) {
                ReviewTemplates.Review review = ReviewTemplates.generate(expected, random);
                Sentiment truth = Sentiment.valueOf(expected.name());
                Sentiment predicted = classifier.predict(review.text()).label();
                confusion[truth.ordinal()][predicted.ordinal()]++;
                if (truth != predicted && errors.size() < 10) {
                    errors.add(truth + " -> " + predicted + ": " + review.text());
                }
            }
        }
        double accuracy = report("IA 1 - Sentimento (avaliações geradas)", labels, confusion, errors);
        assertTrue(accuracy >= MIN_ACCURACY, "Acurácia abaixo do esperado: " + accuracy);
    }

    @Test
    void supportClassifierHandlesGeneratedRequests() throws IOException {
        SupportClassifier classifier = SupportClassifier.train();
        Random random = new Random(2026);
        SupportCategory[] labels = SupportCategory.values();
        int[][] confusion = new int[labels.length][labels.length];
        List<String> errors = new ArrayList<>();

        for (SupportTemplates.Category expected : SupportTemplates.Category.values()) {
            for (int i = 0; i < SAMPLES_PER_CLASS; i++) {
                SupportTemplates.Request request = SupportTemplates.generate(expected, random);
                SupportCategory truth = SupportCategory.valueOf(expected.name());
                SupportCategory predicted = classifier.predict(request.text()).label();
                confusion[truth.ordinal()][predicted.ordinal()]++;
                if (truth != predicted && errors.size() < 10) {
                    errors.add(truth + " -> " + predicted + ": " + request.text());
                }
            }
        }
        double accuracy = report("IA 2 - Suporte (solicitações geradas)", labels, confusion, errors);
        assertTrue(accuracy >= MIN_ACCURACY, "Acurácia abaixo do esperado: " + accuracy);
    }

    private static synchronized double report(String title, Enum<?>[] labels, int[][] confusion, List<String> errors)
            throws IOException {
        int correct = 0;
        int total = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(title).append(" ===").append(System.lineSeparator());
        for (int i = 0; i < labels.length; i++) {
            int rowTotal = 0;
            for (int j = 0; j < labels.length; j++) {
                rowTotal += confusion[i][j];
                total += confusion[i][j];
            }
            correct += confusion[i][i];
            sb.append(String.format(Locale.ROOT, "  %-12s acerto=%6.2f%%  previsões=%s%n", labels[i].name(),
                    100.0 * confusion[i][i] / rowTotal, java.util.Arrays.toString(confusion[i])));
        }
        double accuracy = (double) correct / total;
        sb.append(String.format(Locale.ROOT, "  ACURÁCIA GERAL: %.2f%% (%d/%d)%n", accuracy * 100, correct, total));
        if (!errors.isEmpty()) {
            sb.append("  Exemplos de erros:").append(System.lineSeparator());
            errors.forEach(e -> sb.append("    - ").append(e).append(System.lineSeparator()));
        }
        System.out.print(sb);
        Files.createDirectories(REPORT.getParent());
        Files.writeString(REPORT, sb.toString(), StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        return accuracy;
    }
}
