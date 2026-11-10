package br.com.oficina.ordemservico.entities;

import java.util.Set;

public enum StatusOS {

    RECEBIDA(7) {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Set.of(EM_DIAGNOSTICO);
        }
    },
    EM_DIAGNOSTICO(6) {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Set.of(AGUARDANDO_ORCAMENTO);
        }
    },
    AGUARDANDO_ORCAMENTO(5) {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Set.of(AGUARDANDO_APROVACAO, EM_DIAGNOSTICO);
        }
    },
    AGUARDANDO_APROVACAO(4) {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Set.of(AGUARDANDO_PAGAMENTO, CANCELADA);
        }
    },
    AGUARDANDO_PAGAMENTO(3) {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Set.of(AGUARDANDO_EXECUCAO, CANCELADA);
        }
    },
    AGUARDANDO_EXECUCAO(2) {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Set.of(EM_EXECUCAO, CANCELADA);
        }
    },
    EM_EXECUCAO(1) {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Set.of(FINALIZADA);
        }
    },
    // Estados terminais recebem prioridade 99 — nunca aparecem na listagem operacional.
    FINALIZADA(99) {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Set.of(ENTREGUE);
        }
    },
    ENTREGUE(99) {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Set.of();
        }
    },
    CANCELADA(99) {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Set.of();
        }
    };

    /**
     * Ordem de exibição na fila de trabalho (menor = maior prioridade):
     * EM_EXECUCAO, AGUARDANDO_EXECUCAO, AGUARDANDO_PAGAMENTO, AGUARDANDO_APROVACAO,
     * AGUARDANDO_ORCAMENTO, EM_DIAGNOSTICO, RECEBIDA.
     */
    private final int prioridadeListagem;

    StatusOS(int prioridadeListagem) {
        this.prioridadeListagem = prioridadeListagem;
    }

    public abstract Set<StatusOS> transicoesPermitidas();

    public boolean podeTransicionarPara(StatusOS destino) {
        return transicoesPermitidas().contains(destino);
    }

    /**
     * Só duas transições são manuais (pelo PATCH de status). As demais
     * pertencem à saga e chegam por mensagens dos outros serviços.
     */
    public boolean transicaoManualPermitida(StatusOS destino) {
        return (this == RECEBIDA && destino == EM_DIAGNOSTICO)
                || (this == FINALIZADA && destino == ENTREGUE);
    }

    public int prioridadeListagem() {
        return prioridadeListagem;
    }

    /**
     * Estados terminais são excluídos (logicamente) da listagem operacional:
     * a OS permanece persistida, mas não polui a fila de trabalho.
     */
    public boolean terminal() {
        return this == FINALIZADA || this == ENTREGUE || this == CANCELADA;
    }
}
