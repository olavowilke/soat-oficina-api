package br.com.oficina.saga.usecases;

import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.saga.entities.SagaOrdemServico;
import br.com.oficina.saga.gateways.SagaGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Lista todas as sagas de uma OS, da mais nova para a mais antiga. */
@Service
public class ListarSagasDaOsUseCase {

    private final OrdemServicoGateway ordemServicoGateway;
    private final SagaGateway sagaGateway;

    public ListarSagasDaOsUseCase(OrdemServicoGateway ordemServicoGateway, SagaGateway sagaGateway) {
        this.ordemServicoGateway = ordemServicoGateway;
        this.sagaGateway = sagaGateway;
    }

    @Transactional(readOnly = true)
    public List<SagaOrdemServico> execute(UUID osId) {
        if (ordemServicoGateway.findById(osId).isEmpty()) {
            throw new RecursoNaoEncontradoException("Ordem de Serviço", osId);
        }
        return sagaGateway.buscarPorOsId(osId).stream()
                .sorted(Comparator.comparing(SagaOrdemServico::getCriadoEm).reversed())
                .toList();
    }
}
