package br.com.oficina.saga.entities;

public class EstadoSagaInesperadoException extends IllegalStateException {

    private final EstadoSaga esperado;
    private final EstadoSaga atual;

    public EstadoSagaInesperadoException(EstadoSaga esperado, EstadoSaga atual) {
        super("Estado da saga inesperado: esperado " + esperado + ", atual " + atual);
        this.esperado = esperado;
        this.atual = atual;
    }

    public EstadoSaga getEsperado() { return esperado; }

    public EstadoSaga getAtual() { return atual; }
}
