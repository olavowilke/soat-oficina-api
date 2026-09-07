package br.com.oficina.peca.controllers;

import jakarta.validation.constraints.NotNull;

public record AjustarEstoqueRequest(
        @NotNull Integer quantidade
) {}
