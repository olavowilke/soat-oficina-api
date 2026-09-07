package br.com.oficina.ordemservico.controllers;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CriarOrdemServicoRequest(
        @NotNull UUID clienteId,
        @NotNull UUID veiculoId,
        String observacoes
) {}
