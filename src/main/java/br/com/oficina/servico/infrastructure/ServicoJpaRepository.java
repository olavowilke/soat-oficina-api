package br.com.oficina.servico.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServicoJpaRepository extends JpaRepository<ServicoData, UUID> {
    List<ServicoData> findByAtivoTrue();
}
