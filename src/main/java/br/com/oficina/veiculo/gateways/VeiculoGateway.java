package br.com.oficina.veiculo.gateways;

import br.com.oficina.veiculo.entities.Veiculo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port (Gateway) do contexto de veículos. Isola o núcleo do mecanismo de persistência.
 * A implementação (Infra) traduz entre {@link Veiculo} (domínio) e o modelo JPA.
 */
public interface VeiculoGateway {
    Optional<Veiculo> findById(UUID id);
    Optional<Veiculo> findByPlaca(String placa);
    List<Veiculo> findByClienteId(UUID clienteId);
    boolean existsByPlaca(String placa);
    Veiculo save(Veiculo veiculo);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
