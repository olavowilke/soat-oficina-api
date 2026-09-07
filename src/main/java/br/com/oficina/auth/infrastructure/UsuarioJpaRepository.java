package br.com.oficina.auth.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioData, UUID> {

    Optional<UsuarioData> findByUsername(String username);

    boolean existsByUsername(String username);
}
