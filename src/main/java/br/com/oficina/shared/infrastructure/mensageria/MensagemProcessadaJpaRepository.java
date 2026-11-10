package br.com.oficina.shared.infrastructure.mensageria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MensagemProcessadaJpaRepository extends JpaRepository<MensagemProcessadaData, UUID> {
}
