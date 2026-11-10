package br.com.oficina.saga.usecases;

import br.com.oficina.ordemservico.entities.ItemPeca;
import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.peca.gateways.PecaGateway;
import org.springframework.stereotype.Service;

/**
 * Compensação: devolve ao estoque as peças reservadas pela OS, com UPDATE atômico por item.
 * Só roda dentro de uma mudança de estado da saga, então roda uma vez por saga.
 */
@Service
public class DevolverPecasAoEstoque {

    private final PecaGateway pecaGateway;

    public DevolverPecasAoEstoque(PecaGateway pecaGateway) {
        this.pecaGateway = pecaGateway;
    }

    public void execute(OrdemServico os) {
        for (ItemPeca item : os.getItensPeca()) {
            pecaGateway.devolverAoEstoque(item.getPecaId(), item.getQuantidade());
        }
    }
}
