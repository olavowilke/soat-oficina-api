package br.com.oficina.ordemservico.entities;

import java.util.Set;

public enum StatusOS {

    RECEBIDA(4) {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Set.of(EM_DIAGNOSTICO);
        }
    },
    EM_DIAGNOSTICO(3) {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Set.of(AGUARDANDO_APROVACAO);
        }
    },
    AGUARDANDO_APROVACAO(2) {
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
     * EM_EXECUCAO &gt; AGUARDANDO_APROVACAO &gt; EM_DIAGNOSTICO &gt; RECEBIDA.
     */
    private final int prioridadeListagem;

    StatusOS(int prioridadeListagem) {
        this.prioridadeListagem = prioridadeListagem;
    }

    public abstract Set<StatusOS> transicoesPermitidas();

    public boolean podeTransicionarPara(StatusOS destino) {
        return transicoesPermitidas().contains(destino);
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
