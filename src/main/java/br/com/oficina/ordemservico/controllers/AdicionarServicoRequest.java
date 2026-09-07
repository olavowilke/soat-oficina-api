package br.com.oficina.ordemservico.controllers;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AdicionarServicoRequest(@NotNull UUID servicoId) {}
