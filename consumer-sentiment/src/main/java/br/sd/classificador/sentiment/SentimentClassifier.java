package br.sd.classificador.sentiment;

import br.sd.classificador.common.nlp.LabeledDataset;
import br.sd.classificador.common.nlp.ModelEvaluation;
import br.sd.classificador.common.nlp.NaiveBayesTextModel;
import br.sd.classificador.common.nlp.Prediction;
import br.sd.classificador.common.nlp.TextClassifier;
import br.sd.classificador.common.nlp.TextPreprocessor;

/**
 * IA 1 - Análise de sentimento de avaliações de produtos (POSITIVA, NEGATIVA, NEUTRA).
 * <p>
 * Treinada no startup, dentro do próprio container, a partir do dataset rotulado
 * {@code dataset/sentiment-pt.csv} embutido nos resources. Pipeline: normalização,
 * tokenização, stopwords PT, stemming leve, unigramas+bigramas, bag-of-words (Smile
 * {@code BagOfWords}) e Naive Bayes multinomial (Smile {@code DiscreteNaiveBayes}).
 */
public final class SentimentClassifier implements TextClassifier {

    public static final String DATASET_RESOURCE = "/dataset/sentiment-pt.csv";

    private final LabeledDataset<Sentiment> dataset;
    private final TextPreprocessor preprocessor;
    private final NaiveBayesTextModel<Sentiment> model;

    private SentimentClassifier(LabeledDataset<Sentiment> dataset, TextPreprocessor preprocessor) {
        this.dataset = dataset;
        this.preprocessor = preprocessor;
        this.model = NaiveBayesTextModel.train(dataset.examples(), Sentiment.class, preprocessor);
    }

    /** Carrega o dataset embutido e treina o modelo. */
    public static SentimentClassifier train() {
        return new SentimentClassifier(LabeledDataset.fromResource(DATASET_RESOURCE, Sentiment.class),
                new TextPreprocessor());
    }

    @Override
    public String classify(String texto) {
        return predict(texto).label().name();
    }

    public Prediction<Sentiment> predict(String texto) {
        return model.predict(texto);
    }

    /** Validação cruzada estratificada sobre o dataset de treino. */
    public ModelEvaluation evaluate(int folds) {
        return ModelEvaluation.crossValidate(dataset, folds, 42L, preprocessor);
    }

    public LabeledDataset<Sentiment> dataset() {
        return dataset;
    }

    public int vocabularySize() {
        return model.vocabularySize();
    }
}
