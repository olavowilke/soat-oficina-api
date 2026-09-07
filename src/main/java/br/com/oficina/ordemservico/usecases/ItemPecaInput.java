package br.com.oficina.ordemservico.usecases;

import java.util.UUID;

/** Linha de peça na abertura consolidada de OS: qual peça e em que quantidade. */
public record ItemPecaInput(UUID pecaId, int quantidade) {}
