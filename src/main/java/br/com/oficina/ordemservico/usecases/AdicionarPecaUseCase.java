package br.com.oficina.ordemservico.usecases;

import br.com.oficina.ordemservico.entities.ItemPeca;
import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.peca.entities.Peca;
import br.com.oficina.peca.gateways.PecaGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdicionarPecaUseCase {

    private final OrdemServicoGateway ordemServicoGateway;
    private final PecaGateway pecaGateway;

    public AdicionarPecaUseCase(OrdemServicoGateway ordemServicoGateway, PecaGateway pecaGateway) {
        this.ordemServicoGateway = ordemServicoGateway;
        this.pecaGateway = pecaGateway;
    }

    @Transactional
    public OrdemServico execute(AdicionarPecaCommand command) {
        OrdemServico os = ordemServicoGateway.findById(command.ordemServicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", command.ordemServicoId()));
        Peca peca = pecaGateway.findById(command.pecaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça", command.pecaId()));
        peca.ajustarEstoque(-command.quantidade());
        pecaGateway.save(peca);
        ItemPeca item = ItemPeca.novo(peca.getId(), peca.getNome(), command.quantidade(), peca.getPrecoUnitario());
        os.adicionarPeca(item);
        return ordemServicoGateway.save(os);
    }
}
