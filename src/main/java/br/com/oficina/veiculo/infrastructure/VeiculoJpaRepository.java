package br.com.oficina.veiculo.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeiculoJpaRepository extends JpaRepository<VeiculoData, UUID> {
    Optional<VeiculoData> findByPlaca(String placa);
    boolean existsByPlaca(String placa);
    List<VeiculoData> findByClienteId(UUID clienteId);
}
