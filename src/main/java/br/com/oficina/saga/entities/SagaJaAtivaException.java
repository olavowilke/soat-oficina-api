package br.com.oficina.saga.entities;

import java.util.UUID;

public class SagaJaAtivaException extends RuntimeException {

    public SagaJaAtivaException(UUID osId) {
        super("Já existe uma saga ativa para a ordem de serviço " + osId);
    }
}
