package br.com.oficina.saga.usecases;

import java.util.Map;
import java.util.UUID;

/**
 * Resposta de Billing ou Execution já sem envelope. Sem Jackson: o payload
 * chega como mapa de valores simples (UUID em texto, dinheiro em texto).
 */
public record RespostaSaga(UUID messageId, UUID sagaId, UUID osId, String type, Map<String, Object> payload) {
}
