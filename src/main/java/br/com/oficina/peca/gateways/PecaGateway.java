package br.com.oficina.peca.gateways;

import br.com.oficina.peca.entities.Peca;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port (Gateway) do contexto de peças. Isola o núcleo do mecanismo de persistência.
 * A implementação (Infra) traduz entre {@link Peca} (domínio) e o modelo JPA.
 */
public interface PecaGateway {
    Optional<Peca> findById(UUID id);
    List<Peca> findAll();
    List<Peca> findAllAtivos();
    Peca save(Peca peca);
    boolean existsById(UUID id);
}
