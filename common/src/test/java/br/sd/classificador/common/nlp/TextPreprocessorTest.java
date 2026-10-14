package br.sd.classificador.common.nlp;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextPreprocessorTest {

    private final TextPreprocessor preprocessor = new TextPreprocessor();

    @Test
    void normalizesCaseAccentsAndPunctuation() {
        assertEquals("nao consigo acessar o e mail", preprocessor.normalize("Não consigo ACESSAR o e-mail!!!"));
    }

    @Test
    void removesStopwordsButKeepsNegations() {
        List<String> tokens = preprocessor.tokens("O produto não é bom e a entrega foi lenta");
        assertTrue(tokens.contains("nao"));
        assertFalse(tokens.contains("o"));
        assertFalse(tokens.contains("foi"));
        assertTrue(tokens.contains("lent"));
    }

    @Test
    void lightStemmingUnifiesGenderAndNumber() {
        assertEquals(TextPreprocessor.stem("otimo"), TextPreprocessor.stem("otima"));
        assertEquals(TextPreprocessor.stem("lentas"), TextPreprocessor.stem("lento"));
        assertEquals("nao", TextPreprocessor.stem("nao"));
    }

    @Test
    void producesBigrams() {
        List<String> features = Arrays.asList(preprocessor.features("Não recomendo"));
        assertTrue(features.contains("nao_recomend"), features.toString());
    }

    @Test
    void ignoresNumbersAndHandlesEmptyText() {
        assertFalse(preprocessor.tokens("demora 30 segundos").contains("30"));
        assertEquals(0, preprocessor.features("").length);
        assertEquals(0, preprocessor.features(null).length);
    }
}
