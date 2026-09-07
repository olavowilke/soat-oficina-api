package br.com.oficina.ordemservico.usecases;

import java.util.UUID;

public record CriarOrdemServicoCommand(UUID clienteId, UUID veiculoId, String observacoes) {}
