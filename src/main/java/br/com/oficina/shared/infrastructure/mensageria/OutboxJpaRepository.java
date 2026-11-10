package br.com.oficina.shared.infrastructure.mensageria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxJpaRepository extends JpaRepository<OutboxData, UUID> {

    /**
     * Até 50 linhas não enviadas, as mais antigas primeiro. SKIP LOCKED deixa
     * duas instâncias do relay trabalharem sem pegar a mesma linha.
     */
    @Query(value = "SELECT * FROM outbox WHERE enviado_em IS NULL ORDER BY criado_em, id "
            + "LIMIT 50 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxData> buscarPendentesComTrava();
}
