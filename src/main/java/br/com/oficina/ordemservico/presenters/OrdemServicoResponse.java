package br.com.oficina.ordemservico.presenters;

import br.com.oficina.ordemservico.entities.StatusOS;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrdemServicoResponse(
        UUID id,
        UUID clienteId,
        UUID veiculoId,
        StatusOS status,
        String observacoes,
        List<ItemServicoResponse> itensServico,
        List<ItemPecaResponse> itensPeca,
        BigDecimal orcamentoTotal,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        LocalDateTime inicioExecucao,
        LocalDateTime fimExecucao
) {}
