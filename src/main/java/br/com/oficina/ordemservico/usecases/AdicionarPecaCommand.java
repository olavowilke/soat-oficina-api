package br.com.oficina.ordemservico.usecases;

import java.util.UUID;

public record AdicionarPecaCommand(UUID ordemServicoId, UUID pecaId, int quantidade) {}
