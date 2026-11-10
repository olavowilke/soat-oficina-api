package br.com.oficina.ordemservico.entities;

/**
 * A transição pedida só a saga pode fazer. Estende IllegalStateException,
 * então o GlobalExceptionHandler responde 409.
 */
public class TransicaoControladaPelaSagaException extends IllegalStateException {

    public TransicaoControladaPelaSagaException(StatusOS de, StatusOS para) {
        super("Transição " + de + " → " + para + " é controlada pela saga e não pode ser feita manualmente");
    }
}
