package br.sd.classificador.common.nlp;

import smile.classification.DiscreteNaiveBayes;
import smile.feature.extraction.BagOfWords;
import smile.util.IntSet;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Modelo de classificação de texto treinado com a biblioteca Smile:
 * <ul>
 *   <li>vetorização bag-of-words com {@link BagOfWords} (contagem de termos sobre o vocabulário do treino);</li>
 *   <li>classificador {@link DiscreteNaiveBayes} multinomial com suavização de Laplace.</li>
 * </ul>
 * Naive Bayes multinomial é o algoritmo clássico para classificação de textos curtos,
 * treina em milissegundos, fornece probabilidades a posteriori (confiança) e é 100% Java
 * (não depende de BLAS/LAPACK nativos).
 */
public final class NaiveBayesTextModel<L extends Enum<L>> {

    /** Bigramas precisam aparecer em pelo menos N documentos para entrar no vocabulário. */
    private static final int MIN_BIGRAM_DOC_FREQUENCY = 2;
    /** Suavização de Laplace (add-one). */
    private static final double LAPLACE_SIGMA = 1.0;

    private final Class<L> labelType;
    private final L[] labels;
    private final BagOfWords bagOfWords;
    private final DiscreteNaiveBayes naiveBayes;
    private final int vocabularySize;

    private NaiveBayesTextModel(Class<L> labelType, BagOfWords bagOfWords, DiscreteNaiveBayes naiveBayes,
                                int vocabularySize) {
        this.labelType = labelType;
        this.labels = labelType.getEnumConstants();
        this.bagOfWords = bagOfWords;
        this.naiveBayes = naiveBayes;
        this.vocabularySize = vocabularySize;
    }

    /** Treina o modelo com todos os exemplos informados. */
    public static <L extends Enum<L>> NaiveBayesTextModel<L> train(List<LabeledText<L>> examples, Class<L> labelType,
                                                                   TextPreprocessor preprocessor) {
        if (examples.isEmpty()) {
            throw new IllegalArgumentException("Dataset de treino vazio");
        }
        String[] vocabulary = buildVocabulary(examples, preprocessor);
        BagOfWords bagOfWords = new BagOfWords(preprocessor::features, vocabulary);

        int n = examples.size();
        int[][] x = new int[n][];
        int[] y = new int[n];
        for (int i = 0; i < n; i++) {
            x[i] = bagOfWords.apply(examples.get(i).text());
            y[i] = examples.get(i).label().ordinal();
        }

        int k = labelType.getEnumConstants().length;
        DiscreteNaiveBayes nb = new DiscreteNaiveBayes(DiscreteNaiveBayes.Model.MULTINOMIAL, k, vocabulary.length,
                LAPLACE_SIGMA, IntSet.of(k));
        nb.update(x, y);
        return new NaiveBayesTextModel<>(labelType, bagOfWords, nb, vocabulary.length);
    }

    /** Vocabulário = todos os unigramas do treino + bigramas com frequência mínima. */
    private static <L extends Enum<L>> String[] buildVocabulary(List<LabeledText<L>> examples,
                                                                TextPreprocessor preprocessor) {
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (LabeledText<L> example : examples) {
            Set<String> unique = new HashSet<>();
            Collections.addAll(unique, preprocessor.features(example.text()));
            for (String term : unique) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }
        Set<String> vocabulary = new TreeSet<>();
        documentFrequency.forEach((term, df) -> {
            boolean isBigram = term.contains(TextPreprocessor.BIGRAM_SEPARATOR);
            if (!isBigram || df >= MIN_BIGRAM_DOC_FREQUENCY) {
                vocabulary.add(term);
            }
        });
        return vocabulary.toArray(String[]::new);
    }

    public Prediction<L> predict(String text) {
        int[] x = bagOfWords.apply(text);
        double[] posteriori = new double[labels.length];
        int index = naiveBayes.predict(x, posteriori);
        if (index < 0 || index >= labels.length) {
            // O Smile devolve Integer.MIN_VALUE quando nenhum termo do texto está no vocabulário
            // (vetor todo zero). Nesse caso a melhor estimativa é a probabilidade a priori.
            double[] priori = naiveBayes.priori();
            System.arraycopy(priori, 0, posteriori, 0, labels.length);
            index = 0;
            for (int i = 1; i < labels.length; i++) {
                if (priori[i] > priori[index]) {
                    index = i;
                }
            }
        }
        Map<L, Double> probabilities = new EnumMap<>(labelType);
        for (int i = 0; i < labels.length; i++) {
            probabilities.put(labels[i], posteriori[i]);
        }
        return new Prediction<>(labels[index], posteriori[index], probabilities);
    }

    public int vocabularySize() {
        return vocabularySize;
    }
}
