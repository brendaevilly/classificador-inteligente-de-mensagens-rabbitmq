package br.sd.classificador.generator.templates;

import java.util.List;
import java.util.Random;

/** Utilitários para montar frases a partir de partes. */
final class TextTemplates {

    private TextTemplates() {
    }

    static <T> T pick(List<T> items, Random random) {
        return items.get(random.nextInt(items.size()));
    }

    /** Junta as partes não vazias com espaço e capitaliza a primeira letra. */
    static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(capitalize(part.trim()));
        }
        return sb.toString();
    }

    static String capitalize(String s) {
        if (s.isEmpty() || !Character.isLowerCase(s.charAt(0))) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
