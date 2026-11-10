package br.com.oficina.saga.entities;

public class EstadoSagaInesperadoException extends IllegalStateException {

    public EstadoSagaInesperadoException(EstadoSaga esperado, EstadoSaga atual) {
        super("Estado da saga inesperado: esperado " + esperado + ", atual " + atual);
    }
}
