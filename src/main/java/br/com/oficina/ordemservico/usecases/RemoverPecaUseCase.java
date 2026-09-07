package br.com.oficina.ordemservico.usecases;

import br.com.oficina.ordemservico.entities.ItemPeca;
import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.peca.entities.Peca;
import br.com.oficina.peca.gateways.PecaGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service("removerPecaDaOrdemUseCase")
public class RemoverPecaUseCase {

    private final OrdemServicoGateway ordemServicoGateway;
    private final PecaGateway pecaGateway;

    public RemoverPecaUseCase(OrdemServicoGateway ordemServicoGateway, PecaGateway pecaGateway) {
        this.ordemServicoGateway = ordemServicoGateway;
        this.pecaGateway = pecaGateway;
    }

    @Transactional
    public OrdemServico execute(UUID ordemServicoId, UUID itemPecaId) {
        OrdemServico os = ordemServicoGateway.findById(ordemServicoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", ordemServicoId));
        ItemPeca itemPeca = os.getItensPeca().stream()
                .filter(i -> i.getId().equals(itemPecaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item de peça não encontrado: " + itemPecaId));
        Peca peca = pecaGateway.findById(itemPeca.getPecaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça", itemPeca.getPecaId()));
        peca.ajustarEstoque(itemPeca.getQuantidade());
        pecaGateway.save(peca);
        os.removerPeca(itemPecaId);
        return ordemServicoGateway.save(os);
    }
}
