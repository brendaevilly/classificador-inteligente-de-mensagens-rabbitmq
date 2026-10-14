package br.sd.classificador.generator.templates;

import java.util.List;
import java.util.Random;

import static br.sd.classificador.generator.templates.TextTemplates.join;
import static br.sd.classificador.generator.templates.TextTemplates.pick;

/**
 * Banco de partes para gerar solicitações de suporte técnico das categorias
 * ACESSO, DESEMPENHO e CONEXAO. Cada solicitação combina: saudação + relato do
 * problema (com o sistema afetado) + detalhe específico + detalhe genérico + fechamento.
 */
public final class SupportTemplates {

    public enum Category { ACESSO, DESEMPENHO, CONEXAO }

    /** Solicitação gerada e a categoria do template utilizado. */
    public record Request(String text, Category category) {
    }

    static final List<Noun> SYSTEMS = List.of(
            Noun.m("sistema financeiro"), Noun.m("portal do cliente"), Noun.m("aplicativo móvel"),
            Noun.m("e-mail corporativo"), Noun.m("sistema de RH"), Noun.m("painel administrativo"),
            Noun.m("site da loja"), Noun.m("sistema de vendas"), Noun.m("CRM"), Noun.m("ERP"),
            Noun.f("intranet"), Noun.f("plataforma de ensino"), Noun.f("área do aluno"),
            Noun.f("ferramenta de chamados"));

    static final List<String> OPENINGS = List.of(
            "", "", "olá, equipe de suporte.", "bom dia!", "boa tarde, preciso de ajuda.", "urgente:",
            "chamado aberto:", "prezados,", "oi, pessoal do TI.");

    static final List<String> GENERIC_DETAILS = List.of(
            "", "", "isso começou hoje de manhã.", "acontece desde a última atualização.",
            "outros colegas do setor relatam o mesmo.", "já reiniciei o computador e nada mudou.",
            "preciso resolver isso ainda hoje.");

    static final List<String> CLOSINGS = List.of(
            "aguardo retorno.", "obrigado.", "fico no aguardo.", "agradeço desde já.", "", "");

    // ---------------------------------------------------------------- ACESSO
    static final List<String> ACCESS_PROBLEMS = List.of(
            "não consigo fazer login {no} {p}.",
            "minha senha não é aceita {no} {p}, mesmo digitando corretamente.",
            "minha conta foi bloqueada após várias tentativas de login {no} {p}.",
            "não recebo o código de verificação para entrar {no} {p}.",
            "aparece 'acesso negado' quando tento abrir {o} {p}.",
            "{o} {p} diz que meu usuário ou senha são inválidos.",
            "esqueci minha senha {do} {p} e o link de redefinição não chega no meu e-mail.",
            "meu usuário perdeu as permissões {do} {p}.",
            "a autenticação em dois fatores {do} {p} falha toda vez que tento entrar.",
            "minha sessão {no} {p} expira e sou obrigado a fazer login de novo a cada minuto.",
            "não tenho permissão para abrir os relatórios {do} {p}.",
            "o token de acesso {do} {p} expirou e não consigo renovar.");

    static final List<String> ACCESS_DETAILS = List.of(
            "já tentei redefinir a senha duas vezes.", "preciso que liberem meu acesso com urgência.",
            "meu login é o mesmo de sempre.", "a mensagem diz 'credenciais inválidas'.",
            "o administrador precisa desbloquear meu usuário.", "");

    // ---------------------------------------------------------------- DESEMPENHO
    static final List<String> PERFORMANCE_PROBLEMS = List.of(
            "{o} {p} está extremamente {lento} hoje.",
            "as páginas {do} {p} demoram mais de 30 segundos para carregar.",
            "{o} {p} trava toda vez que abro um relatório.",
            "o computador fica muito lento quando abro {o} {p}.",
            "o relatório mensal {do} {p} leva horas para ser gerado.",
            "{o} {p} congela por alguns minutos e depois volta ao normal.",
            "o uso de CPU chega a 100% com {o} {p} aberto e tudo fica travando.",
            "o tempo de resposta {do} {p} piorou muito.",
            "as consultas {no} {p} estão demorando demais.",
            "a tela {do} {p} demora uma eternidade para responder aos cliques.",
            "{o} {p} consome muita memória e deixa o computador pesado.",
            "salvar um registro {no} {p} leva quase um minuto.");

    static final List<String> PERFORMANCE_DETAILS = List.of(
            "antes funcionava rápido.", "fica impossível trabalhar com essa lentidão.",
            "piora no horário de pico.", "já limpei o cache e continua demorando.",
            "a ampulheta fica girando sem parar.", "");

    // ---------------------------------------------------------------- CONEXAO
    static final List<String> CONNECTION_PROBLEMS = List.of(
            "estou sem conexão com a internet e não consigo abrir {o} {p}.",
            "o Wi-Fi do escritório cai a cada poucos minutos.",
            "a VPN não conecta, fica tentando e dá erro, então não chego {no} {p}.",
            "recebo erro de timeout ao conectar com o servidor {do} {p}.",
            "a rede está instável e a conexão com {o} {p} cai o tempo todo.",
            "aparece 'servidor não encontrado' ao abrir {o} {p}.",
            "o computador não detecta o cabo de rede.",
            "a conexão com o servidor {do} {p} é recusada.",
            "o sinal do Wi-Fi está muito fraco na minha sala.",
            "não consigo me conectar à rede corporativa.",
            "a internet fica caindo durante as videochamadas.",
            "há muita perda de pacotes e a conexão oscila.");

    static final List<String> CONNECTION_DETAILS = List.of(
            "o roteador já foi reiniciado.", "outros sites também não abrem.",
            "o ícone de rede mostra 'sem internet'.", "pelo celular a rede funciona normalmente.",
            "o ping para o servidor não responde.", "");

    private SupportTemplates() {
    }

    /** Gera uma solicitação da categoria informada. */
    public static Request generate(Category category, Random random) {
        Noun system = pick(SYSTEMS, random);
        String problem;
        String specificDetail;
        switch (category) {
            case ACESSO -> {
                problem = pick(ACCESS_PROBLEMS, random);
                specificDetail = pick(ACCESS_DETAILS, random);
            }
            case DESEMPENHO -> {
                problem = pick(PERFORMANCE_PROBLEMS, random);
                specificDetail = pick(PERFORMANCE_DETAILS, random);
            }
            default -> {
                problem = pick(CONNECTION_PROBLEMS, random);
                specificDetail = pick(CONNECTION_DETAILS, random);
            }
        }
        String text = join(
                pick(OPENINGS, random),
                system.render(problem, null),
                specificDetail,
                pick(GENERIC_DETAILS, random),
                pick(CLOSINGS, random));
        return new Request(text, category);
    }

    /** Gera uma solicitação com categoria sorteada uniformemente. */
    public static Request generate(Random random) {
        Category[] values = Category.values();
        return generate(values[random.nextInt(values.length)], random);
    }
}
