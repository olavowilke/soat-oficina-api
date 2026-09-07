package br.com.oficina.ordemservico.gateways;

import br.com.oficina.ordemservico.entities.StatusOS;

import java.util.UUID;

/**
 * Dados necessários para notificar o cliente sobre a mudança de status de uma OS.
 * Estrutura de transporte do port {@link NotificacaoGateway} — sem acoplamento a framework.
 */
public record NotificacaoStatusOrdem(
        UUID ordemServicoId,
        StatusOS novoStatus,
        String destinatarioEmail,
        String destinatarioNome
) {}
