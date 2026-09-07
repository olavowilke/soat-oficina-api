package br.com.oficina.ordemservico.gateways;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.entities.StatusOS;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Port (Gateway) de persistência do agregado Ordem de Serviço. Isola o núcleo
 * (Entities/Use Cases) do mecanismo de persistência. A implementação (Infra)
 * traduz entre {@link OrdemServico} (domínio) e o modelo JPA.
 */
public interface OrdemServicoGateway {

    OrdemServico save(OrdemServico ordemServico);

    Optional<OrdemServico> findById(UUID id);

    List<OrdemServico> findAll();

    List<OrdemServico> findByClienteId(UUID clienteId);

    List<OrdemServico> findByStatus(StatusOS status);

    OptionalDouble tempoMedioExecucaoMinutos();
}
