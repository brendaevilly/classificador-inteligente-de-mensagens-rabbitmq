package br.sd.classificador.common.nlp;

import java.util.Locale;
import java.util.Map;

/**
 * Resultado de uma classificação.
 *
 * @param label         classe prevista
 * @param confidence    probabilidade a posteriori da classe prevista (0..1)
 * @param probabilities probabilidade a posteriori de cada classe
 */
public record Prediction<L extends Enum<L>>(L label, double confidence, Map<L, Double> probabilities) {

    public String confidencePercent() {
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f%%", confidence * 100);
    }
}
