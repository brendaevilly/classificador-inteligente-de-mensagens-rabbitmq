package br.sd.classificador.generator.templates;

import java.util.List;
import java.util.Random;

import static br.sd.classificador.generator.templates.TextTemplates.join;
import static br.sd.classificador.generator.templates.TextTemplates.pick;

/**
 * Banco de partes para gerar avaliações de produtos. Cada avaliação combina:
 * abertura (neutra) + frase principal com produto e adjetivo + comentário + fechamento,
 * resultando em dezenas de milhares de combinações diferentes.
 */
public final class ReviewTemplates {

    public enum Sentiment { POSITIVA, NEGATIVA, NEUTRA }

    /** Avaliação gerada e o sentimento do template utilizado. */
    public record Review(String text, Sentiment sentiment) {
    }

    static final List<Noun> PRODUCTS = List.of(
            Noun.m("fone de ouvido"), Noun.m("notebook"), Noun.m("celular"), Noun.m("liquidificador"),
            Noun.m("tênis de corrida"), Noun.m("relógio"), Noun.m("teclado mecânico"), Noun.m("mouse sem fio"),
            Noun.m("monitor"), Noun.m("carregador"), Noun.m("ventilador"), Noun.m("aspirador de pó"),
            Noun.m("micro-ondas"), Noun.m("smartwatch"), Noun.m("livro"), Noun.m("perfume"),
            Noun.m("colchão"), Noun.m("travesseiro"), Noun.m("secador de cabelo"), Noun.m("videogame"),
            Noun.m("headset"), Noun.m("tablet"),
            Noun.f("cafeteira"), Noun.f("air fryer"), Noun.f("mochila"), Noun.f("câmera"),
            Noun.f("caixa de som"), Noun.f("impressora"), Noun.f("geladeira"), Noun.f("cadeira gamer"),
            Noun.f("camiseta"), Noun.f("panela elétrica"), Noun.f("jaqueta"), Noun.f("luminária"),
            Noun.f("batedeira"), Noun.f("smart TV"), Noun.f("garrafa térmica"), Noun.f("bicicleta"));

    static final List<String> OPENINGS = List.of(
            "", "", "",
            "comprei {o} {p} há duas semanas.",
            "recebi {o} {p} ontem.",
            "uso {o} {p} todos os dias.",
            "ganhei {o} {p} de presente.",
            "depois de um mês de uso, segue minha opinião.",
            "avaliação sincera:",
            "sobre a minha última compra:",
            "pedi {o} {p} pelo site.");

    // ---------------------------------------------------------------- POSITIVA
    static final List<Adjective> POSITIVE_ADJECTIVES = List.of(
            Adjective.of("ótimo", "ótima"), Adjective.of("excelente"), Adjective.of("incrível"),
            Adjective.of("maravilhoso", "maravilhosa"), Adjective.of("perfeito", "perfeita"),
            Adjective.of("sensacional"), Adjective.of("fantástico", "fantástica"),
            Adjective.of("muito bom", "muito boa"), Adjective.of("espetacular"));

    static final List<String> POSITIVE_CORES = List.of(
            "{o} {p} é {adj}.",
            "{o} {p} é simplesmente {adj}!",
            "estou muito feliz com {o} {p}, é {adj}.",
            "que {p} {adj}!",
            "{o} {p} superou todas as minhas expectativas, {adj}.",
            "recomendo {o} {p} sem pensar duas vezes, é {adj}.",
            "{este} {p} é {adj}, adorei.");

    static final List<String> POSITIVE_COMMENTS = List.of(
            "chegou antes do prazo e muito bem embalado.",
            "a qualidade superou o que eu esperava.",
            "vale cada centavo investido.",
            "funciona perfeitamente desde o primeiro dia.",
            "o acabamento é impecável.",
            "o atendimento do vendedor foi nota 10.",
            "já quero comprar outro para dar de presente.",
            "melhor compra que fiz este ano.",
            "a bateria dura muito e o desempenho é ótimo.",
            "muito confortável e fácil de usar.",
            "excelente custo-benefício.",
            "estou encantado com a qualidade.",
            "toda a família adorou.",
            "");

    static final List<String> POSITIVE_CLOSINGS = List.of(
            "recomendo!", "nota 10.", "compraria de novo.", "cinco estrelas.", "amei!", "super recomendo.", "");

    // ---------------------------------------------------------------- NEGATIVA
    static final List<Adjective> NEGATIVE_ADJECTIVES = List.of(
            Adjective.of("péssimo", "péssima"), Adjective.of("horrível"), Adjective.of("terrível"),
            Adjective.of("ruim"), Adjective.of("decepcionante"), Adjective.of("frágil demais"),
            Adjective.of("mal feito", "mal feita"), Adjective.of("uma porcaria"));

    static final List<String> NEGATIVE_CORES = List.of(
            "{o} {p} é {adj}.",
            "{o} {p} é simplesmente {adj}.",
            "que decepção com {o} {p}, é {adj}.",
            "que {p} {adj}!",
            "{o} {p} não atendeu às minhas expectativas, é {adj}.",
            "não recomendo {o} {p} para ninguém, é {adj}.",
            "{este} {p} é {adj}, me arrependi.");

    static final List<String> NEGATIVE_COMMENTS = List.of(
            "parou de funcionar depois de três dias.",
            "veio com defeito e a troca está demorando.",
            "o acabamento é de péssima qualidade.",
            "dinheiro jogado fora.",
            "veio diferente do que foi anunciado.",
            "quebrou na primeira semana de uso.",
            "me arrependi da compra.",
            "o vendedor não resolveu nada.",
            "esquenta demais e faz um barulho insuportável.",
            "a entrega atrasou quase um mês.",
            "quero meu dinheiro de volta.",
            "o material é fraco e descasca fácil.",
            "");

    static final List<String> NEGATIVE_CLOSINGS = List.of(
            "não recomendo.", "nunca mais compro.", "uma estrela já é muito.", "que decepção.", "evitem!",
            "nota zero.", "");

    // ---------------------------------------------------------------- NEUTRA
    static final List<Adjective> NEUTRAL_ADJECTIVES = List.of(
            Adjective.of("razoável"), Adjective.of("mediano", "mediana"), Adjective.of("comum"),
            Adjective.of("aceitável"), Adjective.of("normal"), Adjective.of("simples"),
            Adjective.of("ok"), Adjective.of("regular"));

    static final List<String> NEUTRAL_CORES = List.of(
            "{o} {p} é {adj}.",
            "{o} {p} é {adj}, nada de especial.",
            "{o} {p} cumpre o que promete, sem surpresas.",
            "{o} {p} é {adj} para o dia a dia.",
            "achei {o} {p} {adj}.",
            "nada a declarar sobre {o} {p}, funciona como esperado.",
            "{este} {p} é {adj}, nem mais nem menos.");

    static final List<String> NEUTRAL_COMMENTS = List.of(
            "nem bom nem ruim.",
            "atende ao básico.",
            "pelo preço, é o esperado.",
            "tem pontos positivos e negativos.",
            "ainda estou avaliando, por enquanto está dentro do esperado.",
            "funciona, mas poderia ser melhor.",
            "não me impressionou, mas também não decepcionou.",
            "faz o que precisa, sem grandes diferenciais.",
            "a entrega foi no prazo normal.",
            "é parecido com outros modelos da mesma faixa de preço.",
            "algumas funções são boas, outras deixam a desejar.",
            "");

    static final List<String> NEUTRAL_CLOSINGS = List.of(
            "nota 3 de 5.", "três estrelas.", "cumpre o papel.", "é isso.", "nada demais.", "");

    private ReviewTemplates() {
    }

    /** Gera uma avaliação com o sentimento informado. */
    public static Review generate(Sentiment sentiment, Random random) {
        Noun product = pick(PRODUCTS, random);
        String opening = product.render(pick(OPENINGS, random), null);
        String core;
        String comment;
        String closing;
        switch (sentiment) {
            case POSITIVA -> {
                core = product.render(pick(POSITIVE_CORES, random), pick(POSITIVE_ADJECTIVES, random));
                comment = pick(POSITIVE_COMMENTS, random);
                closing = pick(POSITIVE_CLOSINGS, random);
            }
            case NEGATIVA -> {
                core = product.render(pick(NEGATIVE_CORES, random), pick(NEGATIVE_ADJECTIVES, random));
                comment = pick(NEGATIVE_COMMENTS, random);
                closing = pick(NEGATIVE_CLOSINGS, random);
            }
            default -> {
                core = product.render(pick(NEUTRAL_CORES, random), pick(NEUTRAL_ADJECTIVES, random));
                comment = pick(NEUTRAL_COMMENTS, random);
                closing = pick(NEUTRAL_CLOSINGS, random);
            }
        }
        return new Review(join(opening, core, comment, closing), sentiment);
    }

    /** Gera uma avaliação com sentimento sorteado uniformemente. */
    public static Review generate(Random random) {
        Sentiment[] values = Sentiment.values();
        return generate(values[random.nextInt(values.length)], random);
    }
}
