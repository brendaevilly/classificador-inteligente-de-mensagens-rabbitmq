package br.sd.classificador.support;

/** Classes da IA de classificação de solicitações de suporte. */
public enum SupportCategory {
    ACESSO("PROBLEMA DE ACESSO"),
    DESEMPENHO("PROBLEMA DE DESEMPENHO"),
    CONEXAO("PROBLEMA DE CONEXÃO");

    private final String displayName;

    SupportCategory(String displayName) {
        this.displayName = displayName;
    }

    /** Nome exibido nos logs (ex.: "PROBLEMA DE CONEXÃO"). */
    public String displayName() {
        return displayName;
    }
}
