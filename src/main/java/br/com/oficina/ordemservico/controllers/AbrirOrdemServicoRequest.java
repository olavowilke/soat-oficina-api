package br.com.oficina.ordemservico.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Payload consolidado de abertura de OS: cliente e veículo por ID já cadastrado,
 * mais as listas de serviços e peças a incluir na mesma requisição.
 */
public record AbrirOrdemServicoRequest(
        @NotNull UUID clienteId,
        @NotNull UUID veiculoId,
        String observacoes,
        List<@NotNull UUID> servicoIds,
        @Valid List<AdicionarPecaRequest> pecas
) {}
