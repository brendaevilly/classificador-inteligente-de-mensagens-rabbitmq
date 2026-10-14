package br.sd.classificador.common.nlp;

/** Contrato mínimo de uma IA de classificação de texto. */
public interface TextClassifier {

    /** Retorna o nome da classe prevista para o texto (ex.: "POSITIVA", "PROBLEMA DE ACESSO"). */
    String classify(String texto);
}
