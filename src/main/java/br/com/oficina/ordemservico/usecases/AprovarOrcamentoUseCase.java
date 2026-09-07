package br.com.oficina.ordemservico.usecases;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AprovarOrcamentoUseCase {

    private final OrdemServicoGateway ordemServicoGateway;
    private final NotificadorStatusOrdem notificador;

    public AprovarOrcamentoUseCase(OrdemServicoGateway ordemServicoGateway, NotificadorStatusOrdem notificador) {
        this.ordemServicoGateway = ordemServicoGateway;
        this.notificador = notificador;
    }

    @Transactional
    public OrdemServico execute(UUID ordemServicoId) {
        OrdemServico os = ordemServicoGateway.findById(ordemServicoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", ordemServicoId));
        os.aprovarOrcamento();
        OrdemServico salva = ordemServicoGateway.save(os);
        notificador.notificar(salva);
        return salva;
    }
}
