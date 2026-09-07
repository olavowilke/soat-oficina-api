package br.com.oficina.ordemservico.controllers;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AdicionarPecaRequest(
        @NotNull UUID pecaId,
        @Min(1) int quantidade
) {}
