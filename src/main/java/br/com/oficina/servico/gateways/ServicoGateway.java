package br.com.oficina.servico.gateways;

import br.com.oficina.servico.entities.Servico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port (Gateway) do contexto de serviços. Isola o núcleo do mecanismo de persistência.
 * A implementação (Infra) traduz entre {@link Servico} (domínio) e o modelo JPA.
 */
public interface ServicoGateway {
    Optional<Servico> findById(UUID id);
    List<Servico> findAll();
    List<Servico> findAllAtivos();
    Servico save(Servico servico);
    boolean existsById(UUID id);
}
