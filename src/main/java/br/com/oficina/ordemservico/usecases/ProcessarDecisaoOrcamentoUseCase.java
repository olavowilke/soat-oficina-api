package br.com.oficina.ordemservico.usecases;

import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.entities.StatusOS;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Processa a decisão de orçamento notificada por um sistema externo (webhook).
 * <p>
 * Regras:
 * <ul>
 *   <li>Só transiciona quando a OS está {@link StatusOS#AGUARDANDO_APROVACAO}
 *       (a validação de transição é da própria entidade).</li>
 *   <li><b>Idempotência</b>: se a OS já estiver no estado-alvo da decisão
 *       (EM_EXECUCAO para aprovado, CANCELADA para recusado), a operação é um
 *       no-op bem-sucedido — reentregas do webhook não causam erro nem efeito duplo.</li>
 * </ul>
 */
@Service
public class ProcessarDecisaoOrcamentoUseCase {

    private final OrdemServicoGateway ordemServicoGateway;
    private final NotificadorStatusOrdem notificador;

    public ProcessarDecisaoOrcamentoUseCase(OrdemServicoGateway ordemServicoGateway,
                                            NotificadorStatusOrdem notificador) {
        this.ordemServicoGateway = ordemServicoGateway;
        this.notificador = notificador;
    }

    @Transactional
    public OrdemServico execute(UUID ordemServicoId, DecisaoOrcamento decisao) {
        OrdemServico os = ordemServicoGateway.findById(ordemServicoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de Serviço", ordemServicoId));

        StatusOS atual = os.getStatus();
        return switch (decisao) {
            case APROVADO -> {
                if (atual == StatusOS.EM_EXECUCAO) {
                    yield os; // já aprovada — idempotente, sem notificar de novo
                }
                os.aprovarOrcamento();
                yield salvarENotificar(os);
            }
            case RECUSADO -> {
                if (atual == StatusOS.CANCELADA) {
                    yield os; // já recusada — idempotente, sem notificar de novo
                }
                os.recusarOrcamento();
                yield salvarENotificar(os);
            }
        };
    }

    private OrdemServico salvarENotificar(OrdemServico os) {
        OrdemServico salva = ordemServicoGateway.save(os);
        notificador.notificar(salva);
        return salva;
    }
}
