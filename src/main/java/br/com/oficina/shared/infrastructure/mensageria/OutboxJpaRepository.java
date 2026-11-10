package br.com.oficina.shared.infrastructure.mensageria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxJpaRepository extends JpaRepository<OutboxData, UUID> {

    /**
     * Até 50 linhas prontas para envio, na ordem de gravação. Uma linha só entra se
     * não houver linha anterior pendente da mesma saga (mesmo em outro pod ou em espera
     * de nova tentativa), para manter a ordem dos comandos. SKIP LOCKED deixa dois pods
     * trabalharem sem pegar a mesma linha.
     */
    @Query(value = "SELECT * FROM outbox WHERE enviado_em IS NULL "
            + "AND (proxima_tentativa_em IS NULL OR proxima_tentativa_em <= now()) "
            + "AND NOT EXISTS (SELECT 1 FROM outbox o2 WHERE o2.saga_id = outbox.saga_id "
            + "AND o2.enviado_em IS NULL AND o2.seq < outbox.seq) "
            + "ORDER BY seq LIMIT 50 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxData> buscarPendentesComTrava();
}
