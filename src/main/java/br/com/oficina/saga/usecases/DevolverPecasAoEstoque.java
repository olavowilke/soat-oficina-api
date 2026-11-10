package br.com.oficina.saga.usecases;

import br.com.oficina.ordemservico.entities.ItemPeca;
import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.peca.entities.Peca;
import br.com.oficina.peca.gateways.PecaGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;

/**
 * Compensação: devolve ao estoque as peças reservadas pela OS.
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
            Peca peca = pecaGateway.findById(item.getPecaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Peça", item.getPecaId()));
            peca.ajustarEstoque(item.getQuantidade());
            pecaGateway.save(peca);
        }
    }
}
