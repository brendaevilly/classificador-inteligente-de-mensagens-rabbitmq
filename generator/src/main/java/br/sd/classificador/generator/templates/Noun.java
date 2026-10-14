package br.sd.classificador.generator.templates;

/**
 * Substantivo com gênero gramatical, usado para concordância dos templates
 * ("o notebook é ótimo" / "a cafeteira é ótima").
 */
public record Noun(String name, boolean feminine) {

    public static Noun m(String name) {
        return new Noun(name, false);
    }

    public static Noun f(String name) {
        return new Noun(name, true);
    }

    /**
     * Substitui os marcadores do template:
     * {p} nome, {o} artigo, {no} em+artigo, {do} de+artigo, {este} demonstrativo,
     * {adj} adjetivo concordando em gênero e {lento} (forma do adjetivo "lento/lenta").
     */
    public String render(String template, Adjective adjective) {
        String s = template
                .replace("{p}", name)
                .replace("{o}", feminine ? "a" : "o")
                .replace("{no}", feminine ? "na" : "no")
                .replace("{do}", feminine ? "da" : "do")
                .replace("{este}", feminine ? "esta" : "este")
                .replace("{lento}", feminine ? "lenta" : "lento");
        if (adjective != null) {
            s = s.replace("{adj}", adjective.form(feminine));
        }
        return s;
    }
}
