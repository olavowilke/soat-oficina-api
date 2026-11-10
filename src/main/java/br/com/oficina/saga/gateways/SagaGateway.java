package br.com.oficina.saga.gateways;

import br.com.oficina.saga.entities.SagaOrdemServico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SagaGateway {

    SagaOrdemServico salvar(SagaOrdemServico saga);

    Optional<SagaOrdemServico> buscarPorId(UUID id);

    Optional<SagaOrdemServico> buscarAtivaPorOsId(UUID osId);

    List<SagaOrdemServico> buscarPorOsId(UUID osId);
}
