package br.com.oficina.ordemservico.controllers;

import br.com.oficina.ordemservico.entities.StatusOS;
import jakarta.validation.constraints.NotNull;

public record AvancarStatusRequest(@NotNull StatusOS novoStatus) {}
