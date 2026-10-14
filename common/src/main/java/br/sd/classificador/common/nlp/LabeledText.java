package br.sd.classificador.common.nlp;

/** Exemplo rotulado do dataset de treino. */
public record LabeledText<L extends Enum<L>>(L label, String text) {
}
