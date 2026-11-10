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

    /**
     * Posição na sequência: AGUARDANDO_ORCAMENTO < AGUARDANDO_APROVACAO < AGUARDANDO_PAGAMENTO
     * < AGUARDANDO_EXECUCAO < EM_EXECUCAO. ESTORNANDO vem depois de AGUARDANDO_EXECUCAO, num
     * ramo ao lado de EM_EXECUCAO (mesma posição). Estado terminal está além de tudo.
     */
    private int posicao() {
        return switch (this) {
            case AGUARDANDO_ORCAMENTO -> 0;
            case AGUARDANDO_APROVACAO -> 1;
            case AGUARDANDO_PAGAMENTO -> 2;
            case AGUARDANDO_EXECUCAO -> 3;
            case EM_EXECUCAO, ESTORNANDO -> 4;
            case CONCLUIDA, COMPENSADA, FALHOU -> 5;
        };
    }

    /** Verdadeiro se a saga neste estado ainda não chegou em {@code outro}. */
    public boolean antesDe(EstadoSaga outro) {
        return posicao() < outro.posicao();
    }
}
