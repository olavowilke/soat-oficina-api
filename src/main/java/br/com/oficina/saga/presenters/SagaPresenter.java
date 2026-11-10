package br.com.oficina.saga.presenters;

import br.com.oficina.saga.entities.SagaOrdemServico;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SagaPresenter {

    public SagaResponse present(SagaOrdemServico s) {
        return new SagaResponse(s.getId(), s.getEstado(), s.getOrcamentoId(), s.getPagamentoId(),
                s.getExecucaoId(), s.getLinkPagamento(), s.getMotivoFalha(), s.getCriadoEm(), s.getAtualizadoEm());
    }

    public List<SagaResponse> present(List<SagaOrdemServico> sagas) {
        return sagas.stream().map(this::present).toList();
    }
}
