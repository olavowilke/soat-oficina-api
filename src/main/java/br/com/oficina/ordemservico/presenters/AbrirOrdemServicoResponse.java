package br.com.oficina.ordemservico.presenters;

import br.com.oficina.ordemservico.entities.StatusOS;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response da abertura consolidada de OS. Expõe como campo principal a
 * identificação única da OS recém-criada, acompanhada do status inicial e do
 * orçamento total já calculado a partir dos itens informados.
 */
public record AbrirOrdemServicoResponse(UUID ordemServicoId, StatusOS status, BigDecimal orcamentoTotal) {}
