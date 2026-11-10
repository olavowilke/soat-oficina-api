package br.com.oficina.saga.presenters;

import br.com.oficina.saga.entities.EstadoSaga;

import java.time.Instant;
import java.util.UUID;

public record SagaResponse(
        UUID id,
        EstadoSaga estado,
        UUID orcamentoId,
        UUID pagamentoId,
        UUID execucaoId,
        String linkPagamento,
        String motivoFalha,
        Instant criadoEm,
        Instant atualizadoEm
) {}
