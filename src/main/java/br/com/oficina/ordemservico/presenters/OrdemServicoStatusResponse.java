package br.com.oficina.ordemservico.presenters;

import br.com.oficina.ordemservico.entities.StatusOS;

import java.util.UUID;

/**
 * Response reduzido do endpoint público de status — expõe apenas o id da OS e o
 * status atual, omitindo preços, itens e demais dados internos que não devem
 * aparecer em um endpoint não autenticado.
 */
public record OrdemServicoStatusResponse(UUID id, StatusOS status) {}
