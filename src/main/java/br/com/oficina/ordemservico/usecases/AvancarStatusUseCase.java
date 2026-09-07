package br.com.oficina.ordemservico.usecases;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AvancarStatusUseCase {

    private final OrdemServicoGateway ordemServicoGateway;
    private final NotificadorStatusOrdem notificador;

    public AvancarStatusUseCase(OrdemServicoGateway ordemServicoGateway, NotificadorStatusOrdem notificador) {
        this.ordemServicoGateway = ordemServicoGateway;
        this.notificador = notificador;
    }

    @Transactional
    public OrdemServico execute(UUID ordemServicoId, StatusOS novoStatus) {
        OrdemServico os = ordemServicoGateway.findById(ordemServicoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", ordemServicoId));
        os.avancarStatus(novoStatus);
        OrdemServico salva = ordemServicoGateway.save(os);
        notificador.notificar(salva);
        return salva;
    }
}
