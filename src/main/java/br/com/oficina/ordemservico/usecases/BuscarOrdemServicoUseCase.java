package br.com.oficina.ordemservico.usecases;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Busca uma OS pelo id. Atende tanto a consulta autenticada completa quanto a
 * consulta pública de status (o Presenter escolhe a visão exposta em cada caso).
 */
@Service
public class BuscarOrdemServicoUseCase {

    private final OrdemServicoGateway ordemServicoGateway;

    public BuscarOrdemServicoUseCase(OrdemServicoGateway ordemServicoGateway) {
        this.ordemServicoGateway = ordemServicoGateway;
    }

    @Transactional(readOnly = true)
    public OrdemServico execute(UUID id) {
        return ordemServicoGateway.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", id));
    }
}
