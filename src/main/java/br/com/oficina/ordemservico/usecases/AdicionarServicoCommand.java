package br.com.oficina.ordemservico.usecases;

import java.util.UUID;

public record AdicionarServicoCommand(UUID ordemServicoId, UUID servicoId) {}
