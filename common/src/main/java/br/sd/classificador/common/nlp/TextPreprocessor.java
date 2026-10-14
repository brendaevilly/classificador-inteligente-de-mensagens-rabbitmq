package br.sd.classificador.common.nlp;

import smile.nlp.normalizer.SimpleNormalizer;
import smile.nlp.tokenizer.SimpleTokenizer;
import smile.nlp.tokenizer.Tokenizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pré-processamento de texto em português:
 * <ol>
 *   <li>normalização Unicode/espaços com {@link SimpleNormalizer} (Smile);</li>
 *   <li>minúsculas e remoção de acentos (NFD + remoção de diacríticos);</li>
 *   <li>remoção de pontuação e números;</li>
 *   <li>tokenização com {@link SimpleTokenizer} (Smile);</li>
 *   <li>remoção de stopwords em português (lista própria, que preserva negações e
 *       intensificadores como "não", "nada", "muito", "mas", "sem");</li>
 *   <li>stemming leve (remove plural e vogal final: "lentas" -> "lent", "ótimo" -> "otim");</li>
 *   <li>geração de bigramas ("nao_funciona" -> "nao_funcion") para capturar negações.</li>
 * </ol>
 */
public final class TextPreprocessor {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern NUMBER = Pattern.compile("\\d+");
    private static final String STOPWORDS_RESOURCE = "/nlp/stopwords-pt.txt";

    /** Separador usado nos bigramas (não aparece em tokens normalizados). */
    public static final String BIGRAM_SEPARATOR = "_";

    private final SimpleNormalizer normalizer = SimpleNormalizer.getInstance();
    private final Tokenizer tokenizer = new SimpleTokenizer(false);
    private final Set<String> stopwords;
    private final boolean bigrams;

    public TextPreprocessor() {
        this(loadStopwords(), true);
    }

    public TextPreprocessor(Set<String> stopwords, boolean bigrams) {
        this.stopwords = Set.copyOf(stopwords);
        this.bigrams = bigrams;
    }

    /** Minúsculas, sem acentos, sem pontuação e com espaços simples. */
    public String normalize(String text) {
        if (text == null) {
            return "";
        }
        String s = normalizer.normalize(text).toLowerCase(Locale.forLanguageTag("pt-BR"));
        s = DIACRITICS.matcher(Normalizer.normalize(s, Normalizer.Form.NFD)).replaceAll("");
        s = NON_ALPHANUMERIC.matcher(s).replaceAll(" ");
        return s.trim();
    }

    /** Tokens já filtrados (sem stopwords, sem números, com stemming leve). */
    public List<String> tokens(String text) {
        String normalized = normalize(text);
        List<String> result = new ArrayList<>();
        if (normalized.isEmpty()) {
            return result;
        }
        for (String token : tokenizer.split(normalized)) {
            if (token.length() < 2 || NUMBER.matcher(token).matches() || stopwords.contains(token)) {
                continue;
            }
            result.add(stem(token));
        }
        return result;
    }

    /** Features finais: unigramas + bigramas de tokens consecutivos. */
    public String[] features(String text) {
        List<String> tokens = tokens(text);
        List<String> features = new ArrayList<>(tokens);
        if (bigrams) {
            for (int i = 0; i + 1 < tokens.size(); i++) {
                features.add(tokens.get(i) + BIGRAM_SEPARATOR + tokens.get(i + 1));
            }
        }
        return features.toArray(String[]::new);
    }

    /**
     * Stemmer leve para português: remove o "s" final de plural e a vogal temática final
     * (a/o/e), unificando gênero e número sem a agressividade de um stemmer completo.
     */
    static String stem(String token) {
        String t = token;
        if (t.length() > 4 && t.endsWith("s")) {
            t = t.substring(0, t.length() - 1);
        }
        if (t.length() > 4) {
            char last = t.charAt(t.length() - 1);
            if (last == 'a' || last == 'o' || last == 'e') {
                t = t.substring(0, t.length() - 1);
            }
        }
        return t;
    }

    public static Set<String> loadStopwords() {
        Set<String> words = new HashSet<>();
        try (InputStream in = TextPreprocessor.class.getResourceAsStream(STOPWORDS_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Recurso não encontrado: " + STOPWORDS_RESOURCE);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    words.add(line);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return words;
    }
}
