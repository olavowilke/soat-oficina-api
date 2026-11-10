package br.com.oficina.shared.infrastructure.mensageria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox")
@Getter
@Setter
@NoArgsConstructor
public class OutboxData {

    @Id
    private UUID id;

    @Column(nullable = false, length = 30)
    private String destino;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(name = "saga_id", nullable = false)
    private UUID sagaId;

    @Column(name = "os_id", nullable = false)
    private UUID osId;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    /** JSON do payload, como texto. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String payload;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "enviado_em")
    private Instant enviadoEm;

    /** Ordem de gravação, gerada pelo banco. */
    @Column(insertable = false, updatable = false)
    private Long seq;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "proxima_tentativa_em")
    private Instant proximaTentativaEm;
}
