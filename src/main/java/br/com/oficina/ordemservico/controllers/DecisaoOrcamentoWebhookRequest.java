package br.com.oficina.ordemservico.controllers;

import br.com.oficina.ordemservico.usecases.DecisaoOrcamento;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Payload da notificação externa de decisão de orçamento.
 */
public record DecisaoOrcamentoWebhookRequest(
        @NotNull UUID ordemServicoId,
        @NotNull DecisaoOrcamento decisao
) {}
