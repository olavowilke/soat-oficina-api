package br.com.oficina.shared.infrastructure.mensageria;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope de toda mensagem da saga (ver contrato de mensageria da Fase 4).
 * Existe só na infraestrutura: entities e usecases não o conhecem.
 */
public record MensagemSaga(
        UUID messageId,
        UUID sagaId,
        UUID osId,
        String type,
        String correlationId,
        Instant occurredAt,
        JsonNode payload) {
}
