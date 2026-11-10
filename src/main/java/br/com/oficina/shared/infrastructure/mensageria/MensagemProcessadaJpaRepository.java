package br.com.oficina.shared.infrastructure.mensageria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface MensagemProcessadaJpaRepository extends JpaRepository<MensagemProcessadaData, UUID> {

    /** Registra a mensagem. Devolve 0 se outra cópia já a registrou (sem erro de chave duplicada). */
    @Modifying
    @Query(value = "INSERT INTO mensagens_processadas (message_id, processada_em) VALUES (:id, :em) "
            + "ON CONFLICT (message_id) DO NOTHING", nativeQuery = true)
    int registrar(@Param("id") UUID id, @Param("em") Instant em);
}
