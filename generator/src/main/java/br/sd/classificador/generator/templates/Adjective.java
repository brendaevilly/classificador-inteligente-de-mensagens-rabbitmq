package br.sd.classificador.generator.templates;

/** Adjetivo com formas masculina e feminina. */
public record Adjective(String masculine, String feminine) {

    /** Adjetivo invariável em gênero (ex.: "excelente"). */
    public static Adjective of(String invariant) {
        return new Adjective(invariant, invariant);
    }

    public static Adjective of(String masculine, String feminine) {
        return new Adjective(masculine, feminine);
    }

    public String form(boolean femininePlace) {
        return femininePlace ? feminine : masculine;
    }
}
