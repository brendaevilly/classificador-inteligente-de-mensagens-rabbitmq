package br.sd.classificador.support;

import br.sd.classificador.common.nlp.LabeledDataset;
import br.sd.classificador.common.nlp.ModelEvaluation;
import br.sd.classificador.common.nlp.NaiveBayesTextModel;
import br.sd.classificador.common.nlp.Prediction;
import br.sd.classificador.common.nlp.TextClassifier;
import br.sd.classificador.common.nlp.TextPreprocessor;

/**
 * IA 2 - Classificação de solicitações de suporte em PROBLEMA DE ACESSO,
 * PROBLEMA DE DESEMPENHO ou PROBLEMA DE CONEXÃO.
 * <p>
 * Modelo próprio e independente do Consumidor 1: treinado no startup deste container a
 * partir do dataset {@code dataset/support-pt.csv}, com o mesmo tipo de pipeline
 * (bag-of-words do Smile + Naive Bayes multinomial do Smile).
 */
public final class SupportClassifier implements TextClassifier {

    public static final String DATASET_RESOURCE = "/dataset/support-pt.csv";

    private final LabeledDataset<SupportCategory> dataset;
    private final TextPreprocessor preprocessor;
    private final NaiveBayesTextModel<SupportCategory> model;

    private SupportClassifier(LabeledDataset<SupportCategory> dataset, TextPreprocessor preprocessor) {
        this.dataset = dataset;
        this.preprocessor = preprocessor;
        this.model = NaiveBayesTextModel.train(dataset.examples(), SupportCategory.class, preprocessor);
    }

    /** Carrega o dataset embutido e treina o modelo. */
    public static SupportClassifier train() {
        return new SupportClassifier(LabeledDataset.fromResource(DATASET_RESOURCE, SupportCategory.class),
                new TextPreprocessor());
    }

    /** Retorna "PROBLEMA DE ACESSO", "PROBLEMA DE DESEMPENHO" ou "PROBLEMA DE CONEXÃO". */
    @Override
    public String classify(String texto) {
        return predict(texto).label().displayName();
    }

    public Prediction<SupportCategory> predict(String texto) {
        return model.predict(texto);
    }

    /** Validação cruzada estratificada sobre o dataset de treino. */
    public ModelEvaluation evaluate(int folds) {
        return ModelEvaluation.crossValidate(dataset, folds, 42L, preprocessor);
    }

    public LabeledDataset<SupportCategory> dataset() {
        return dataset;
    }

    public int vocabularySize() {
        return model.vocabularySize();
    }
}
