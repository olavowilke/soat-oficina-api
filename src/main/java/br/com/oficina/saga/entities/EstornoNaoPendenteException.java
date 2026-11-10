package br.com.oficina.saga.entities;

import java.util.UUID;

/** A OS não tem um estorno falho esperando o operador. Vira 409. */
public class EstornoNaoPendenteException extends IllegalStateException {

    public EstornoNaoPendenteException(UUID osId) {
        super("A ordem de serviço " + osId + " não tem estorno falho para tratar");
    }
}
