package br.com.oficina.shared.infrastructure.mensageria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Registro de idempotência: um {@code messageId} já tratado não é tratado de novo. */
@Entity
@Table(name = "mensagens_processadas")
@Getter
@Setter
@NoArgsConstructor
public class MensagemProcessadaData {

    @Id
    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "processada_em", nullable = false)
    private Instant processadaEm;
}
