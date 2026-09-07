package br.com.oficina.ordemservico.usecases;

import br.com.oficina.ordemservico.entities.ItemServico;
import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.servico.entities.Servico;
import br.com.oficina.servico.gateways.ServicoGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdicionarServicoUseCase {

    private final OrdemServicoGateway ordemServicoGateway;
    private final ServicoGateway servicoGateway;

    public AdicionarServicoUseCase(OrdemServicoGateway ordemServicoGateway, ServicoGateway servicoGateway) {
        this.ordemServicoGateway = ordemServicoGateway;
        this.servicoGateway = servicoGateway;
    }

    @Transactional
    public OrdemServico execute(AdicionarServicoCommand command) {
        OrdemServico os = ordemServicoGateway.findById(command.ordemServicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", command.ordemServicoId()));
        Servico servico = servicoGateway.findById(command.servicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", command.servicoId()));
        ItemServico item = ItemServico.novo(servico.getId(), servico.getNome(), servico.getPreco());
        os.adicionarServico(item);
        return ordemServicoGateway.save(os);
    }
}
