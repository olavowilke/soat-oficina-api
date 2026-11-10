package br.com.oficina.saga.gateways;

import br.com.oficina.saga.entities.EstadoSaga;

/** Porta de saída para as métricas da saga. A implementação fica na infraestrutura. */
public interface MetricasSagaGateway {

    /** Registra que a saga chegou a um estado terminal (CONCLUIDA, COMPENSADA ou FALHOU). */
    void sagaFinalizada(EstadoSaga estadoFinal);
}
