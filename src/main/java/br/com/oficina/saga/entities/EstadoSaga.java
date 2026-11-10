package br.com.oficina.saga.entities;

public enum EstadoSaga {
    AGUARDANDO_ORCAMENTO,
    AGUARDANDO_APROVACAO,
    AGUARDANDO_PAGAMENTO,
    AGUARDANDO_EXECUCAO,
    EM_EXECUCAO,
    ESTORNANDO,
    CONCLUIDA,
    COMPENSADA,
    FALHOU;

    /** Uma saga ativa ainda espera uma resposta. No máximo uma por OS. */
    public boolean ativo() {
        return switch (this) {
            case AGUARDANDO_ORCAMENTO, AGUARDANDO_APROVACAO, AGUARDANDO_PAGAMENTO,
                 AGUARDANDO_EXECUCAO, EM_EXECUCAO, ESTORNANDO -> true;
            case CONCLUIDA, COMPENSADA, FALHOU -> false;
        };
    }
}
