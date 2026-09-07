package br.com.oficina.ordemservico.presenters;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemServicoResponse(UUID id, UUID servicoId, String nomeServico, BigDecimal valorCobrado) {}
