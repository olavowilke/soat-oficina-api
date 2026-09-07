package br.com.oficina.ordemservico.presenters;

import br.com.oficina.ordemservico.entities.OrdemServico;
import org.springframework.stereotype.Component;

/**
 * Presenter da visão pública de status: monta o {@link OrdemServicoStatusResponse}
 * enxuto (apenas id e status) exposto no endpoint público não autenticado.
 */
@Component
public class StatusOSPresenter {

    public OrdemServicoStatusResponse present(OrdemServico os) {
        return new OrdemServicoStatusResponse(os.getId(), os.getStatus());
    }
}
