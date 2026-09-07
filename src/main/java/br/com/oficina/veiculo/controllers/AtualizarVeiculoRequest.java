package br.com.oficina.veiculo.controllers;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AtualizarVeiculoRequest(
        @NotBlank String marca,
        @NotBlank String modelo,
        @NotNull @Min(1900) Integer ano,
        String cor
) {}
