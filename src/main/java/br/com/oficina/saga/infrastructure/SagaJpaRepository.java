package br.com.oficina.saga.infrastructure;

import br.com.oficina.saga.entities.EstadoSaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SagaJpaRepository extends JpaRepository<SagaData, UUID> {

    Optional<SagaData> findFirstByOsIdAndEstadoIn(UUID osId, Collection<EstadoSaga> estados);

    List<SagaData> findByOsIdOrderByCriadoEmAsc(UUID osId);

    long countByEstado(EstadoSaga estado);
}
