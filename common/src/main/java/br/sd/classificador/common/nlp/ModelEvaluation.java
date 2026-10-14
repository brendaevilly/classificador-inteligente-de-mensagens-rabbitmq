package br.sd.classificador.common.nlp;

import smile.validation.metric.Accuracy;
import smile.validation.metric.ConfusionMatrix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Validação cruzada estratificada k-fold: para cada fold, treina com os demais e avalia
 * no fold separado. A acurácia e a matriz de confusão são calculadas com as métricas do
 * Smile ({@link Accuracy}, {@link ConfusionMatrix}).
 *
 * @param folds     número de folds
 * @param samples   total de exemplos avaliados
 * @param accuracy  acurácia média (0..1)
 * @param confusion matriz de confusão (linhas = real, colunas = previsto)
 */
public record ModelEvaluation(int folds, int samples, double accuracy, int[][] confusion) {

    public static <L extends Enum<L>> ModelEvaluation crossValidate(LabeledDataset<L> dataset, int folds, long seed,
                                                                    TextPreprocessor preprocessor) {
        Class<L> labelType = dataset.labelType();

        // Estratificação: embaralha cada classe e distribui seus exemplos pelos folds em rodízio.
        Map<L, List<LabeledText<L>>> byLabel = new EnumMap<>(labelType);
        for (LabeledText<L> example : dataset.examples()) {
            byLabel.computeIfAbsent(example.label(), l -> new ArrayList<>()).add(example);
        }
        List<List<LabeledText<L>>> foldData = new ArrayList<>();
        for (int f = 0; f < folds; f++) {
            foldData.add(new ArrayList<>());
        }
        Random random = new Random(seed);
        for (List<LabeledText<L>> group : byLabel.values()) {
            List<LabeledText<L>> shuffled = new ArrayList<>(group);
            Collections.shuffle(shuffled, random);
            for (int i = 0; i < shuffled.size(); i++) {
                foldData.get(i % folds).add(shuffled.get(i));
            }
        }

        int total = dataset.size();
        int[] truth = new int[total];
        int[] predicted = new int[total];
        int pos = 0;
        for (int f = 0; f < folds; f++) {
            List<LabeledText<L>> train = new ArrayList<>();
            for (int g = 0; g < folds; g++) {
                if (g != f) {
                    train.addAll(foldData.get(g));
                }
            }
            NaiveBayesTextModel<L> model = NaiveBayesTextModel.train(train, labelType, preprocessor);
            for (LabeledText<L> test : foldData.get(f)) {
                truth[pos] = test.label().ordinal();
                predicted[pos] = model.predict(test.text()).label().ordinal();
                pos++;
            }
        }

        double accuracy = Accuracy.of(truth, predicted);
        int[][] matrix = ConfusionMatrix.of(truth, predicted).matrix;
        return new ModelEvaluation(folds, total, accuracy, matrix);
    }

    public String accuracyPercent() {
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f%%", accuracy * 100);
    }

    /** Matriz de confusão formatada para log. */
    public <L extends Enum<L>> String formatConfusion(Class<L> labelType) {
        L[] labels = labelType.getEnumConstants();
        StringBuilder sb = new StringBuilder("real \\ previsto");
        for (L label : labels) {
            sb.append(String.format(" | %10s", label.name()));
        }
        for (int i = 0; i < labels.length; i++) {
            sb.append(System.lineSeparator()).append(String.format("%15s", labels[i].name()));
            for (int j = 0; j < labels.length; j++) {
                int value = (i < confusion.length && j < confusion[i].length) ? confusion[i][j] : 0;
                sb.append(String.format(" | %10d", value));
            }
        }
        return sb.toString();
    }
}
