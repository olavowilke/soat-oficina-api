package br.com.oficina.cliente.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteJpaRepository extends JpaRepository<ClienteData, UUID> {
    Optional<ClienteData> findByDocumento(String documento);
    boolean existsByDocumento(String documento);
    List<ClienteData> findByAtivoTrue();
}
