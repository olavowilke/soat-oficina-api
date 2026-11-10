package br.com.oficina.saga.usecases;

import br.com.oficina.saga.entities.EstadoSaga;

import java.util.UUID;

/**
 * A resposta chegou antes de a saga alcançar o estado que ela espera. A mensagem não é
 * confirmada: o SQS entrega de novo depois do visibility timeout.
 */
public class RespostaAdiantadaException extends RuntimeException {

    public RespostaAdiantadaException(UUID sagaId, String tipo, EstadoSaga atual, EstadoSaga esperado) {
        super("Resposta " + tipo + " adiantada: saga " + sagaId + " em " + atual + ", espera " + esperado);
    }
}
