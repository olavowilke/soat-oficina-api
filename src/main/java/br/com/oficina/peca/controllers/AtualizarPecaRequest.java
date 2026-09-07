package br.com.oficina.peca.controllers;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AtualizarPecaRequest(
        @NotBlank String nome,
        String descricao,
        @NotNull @DecimalMin("0.01") BigDecimal precoUnitario
) {}
