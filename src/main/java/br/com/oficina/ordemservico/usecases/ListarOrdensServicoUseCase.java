package br.com.oficina.ordemservico.usecases;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ListarOrdensServicoUseCase {

    /**
     * Ordem da fila de trabalho exigida pelo spec: por prioridade de status
     * (EM_EXECUCAO &gt; AGUARDANDO_APROVACAO &gt; EM_DIAGNOSTICO &gt; RECEBIDA) e,
     * dentro do mesmo status, as mais antigas primeiro (data de criação ascendente).
     */
    private static final Comparator<OrdemServico> ORDEM_FILA =
            Comparator.<OrdemServico>comparingInt(os -> os.getStatus().prioridadeListagem())
                    .thenComparing(OrdemServico::getCriadoEm, Comparator.nullsLast(Comparator.naturalOrder()));

    private final OrdemServicoGateway ordemServicoGateway;

    public ListarOrdensServicoUseCase(OrdemServicoGateway ordemServicoGateway) {
        this.ordemServicoGateway = ordemServicoGateway;
    }

    /**
     * Lista ordens de serviço aplicando a regra de negócio de listagem operacional:
     * <ul>
     *   <li>Filtro opcional por {@code clienteId} e por {@code status}.</li>
     *   <li><b>Exclusão lógica</b>: quando <em>não</em> há filtro explícito de status,
     *       as OS terminais (FINALIZADA, ENTREGUE, CANCELADA) são omitidas da fila —
     *       permanecem persistidas, apenas não poluem a listagem operacional.</li>
     *   <li><b>Ordenação</b>: por prioridade de status e, no empate, mais antigas primeiro.</li>
     * </ul>
     * Um filtro explícito por {@code status} (inclusive terminal) devolve exatamente
     * aquele status, permitindo consultar OS já finalizadas quando desejado.
     */
    @Transactional(readOnly = true)
    public List<OrdemServico> execute(UUID clienteId, StatusOS status) {
        if (status != null) {
            return ordemServicoGateway.findByStatus(status).stream()
                    .filter(os -> clienteId == null || clienteId.equals(os.getClienteId()))
                    .sorted(ORDEM_FILA)
                    .toList();
        }

        List<OrdemServico> base = clienteId != null
                ? ordemServicoGateway.findByClienteId(clienteId)
                : ordemServicoGateway.findAll();

        return base.stream()
                .filter(os -> !os.getStatus().terminal())
                .sorted(ORDEM_FILA)
                .toList();
    }
}
