package br.sd.classificador.common.nlp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Carrega um dataset rotulado de um recurso do classpath no formato:
 * <pre>
 * label;text
 * POSITIVA;Adorei o produto, chegou rápido.
 * </pre>
 * Linhas vazias e linhas iniciadas por '#' são ignoradas.
 */
public final class LabeledDataset<L extends Enum<L>> {

    private final List<LabeledText<L>> examples;
    private final Class<L> labelType;

    private LabeledDataset(List<LabeledText<L>> examples, Class<L> labelType) {
        this.examples = Collections.unmodifiableList(examples);
        this.labelType = labelType;
    }

    public static <L extends Enum<L>> LabeledDataset<L> fromResource(String resource, Class<L> labelType) {
        List<LabeledText<L>> examples = new ArrayList<>();
        try (InputStream in = LabeledDataset.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Dataset não encontrado no classpath: " + resource);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || (lineNumber == 1 && trimmed.startsWith("label;"))) {
                    continue;
                }
                int sep = trimmed.indexOf(';');
                if (sep <= 0 || sep == trimmed.length() - 1) {
                    throw new IllegalStateException("Linha " + lineNumber + " inválida em " + resource + ": " + line);
                }
                String label = trimmed.substring(0, sep).strip();
                String text = trimmed.substring(sep + 1).strip();
                try {
                    examples.add(new LabeledText<>(Enum.valueOf(labelType, label), text));
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException("Rótulo desconhecido '" + label + "' na linha " + lineNumber
                            + " de " + resource, e);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new LabeledDataset<>(examples, labelType);
    }

    public List<LabeledText<L>> examples() {
        return examples;
    }

    public Class<L> labelType() {
        return labelType;
    }

    public int size() {
        return examples.size();
    }

    public Map<L, Integer> countByLabel() {
        Map<L, Integer> counts = new EnumMap<>(labelType);
        for (L label : labelType.getEnumConstants()) {
            counts.put(label, 0);
        }
        for (LabeledText<L> example : examples) {
            counts.merge(example.label(), 1, Integer::sum);
        }
        return counts;
    }
}
