package br.com.oficina.ordemservico.presenters;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemPecaResponse(UUID id, UUID pecaId, String nomePeca, int quantidade,
                               BigDecimal valorUnitario, BigDecimal subtotal) {}
