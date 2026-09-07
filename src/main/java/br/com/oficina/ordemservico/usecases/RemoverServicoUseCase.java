package br.com.oficina.ordemservico.usecases;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service("removerServicoDaOrdemUseCase")
public class RemoverServicoUseCase {

    private final OrdemServicoGateway ordemServicoGateway;

    public RemoverServicoUseCase(OrdemServicoGateway ordemServicoGateway) {
        this.ordemServicoGateway = ordemServicoGateway;
    }

    @Transactional
    public OrdemServico execute(UUID ordemServicoId, UUID itemServicoId) {
        OrdemServico os = ordemServicoGateway.findById(ordemServicoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", ordemServicoId));
        os.removerServico(itemServicoId);
        return ordemServicoGateway.save(os);
    }
}
