package br.com.oficina.ordemservico.usecases;

import java.util.List;
import java.util.UUID;

/**
 * Comando da abertura consolidada de OS: cliente e veículo (por ID já cadastrado),
 * observações e as listas de serviços e peças a incluir na mesma operação.
 */
public record AbrirOrdemServicoCommand(
        UUID clienteId,
        UUID veiculoId,
        String observacoes,
        List<UUID> servicoIds,
        List<ItemPecaInput> pecas
) {
    public AbrirOrdemServicoCommand {
        servicoIds = servicoIds == null ? List.of() : List.copyOf(servicoIds);
        pecas = pecas == null ? List.of() : List.copyOf(pecas);
    }
}
