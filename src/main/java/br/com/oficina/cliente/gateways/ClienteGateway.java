package br.com.oficina.cliente.gateways;

import br.com.oficina.cliente.entities.Cliente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port (Gateway) do contexto de clientes. Isola o núcleo do mecanismo de persistência.
 * A implementação (Infra) traduz entre {@link Cliente} (domínio) e o modelo JPA.
 */
public interface ClienteGateway {
    Optional<Cliente> findById(UUID id);
    Optional<Cliente> findByDocumento(String numero);
    boolean existsByDocumento(String numero);
    List<Cliente> findAll();
    List<Cliente> findAllAtivos();
    Cliente save(Cliente cliente);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
