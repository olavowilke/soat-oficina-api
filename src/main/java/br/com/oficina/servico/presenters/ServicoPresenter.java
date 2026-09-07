package br.com.oficina.servico.presenters;

import br.com.oficina.servico.entities.Servico;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Presenter do contexto de serviços: converte a saída dos Use Cases ({@link Servico})
 * no ViewModel {@link ServicoResponse}. Não conhece HTTP/{@code ResponseEntity}.
 */
@Component
public class ServicoPresenter {

    public ServicoResponse present(Servico servico) {
        return new ServicoResponse(
                servico.getId(),
                servico.getNome(),
                servico.getDescricao(),
                servico.getPreco(),
                servico.getTempoEstimadoMinutos(),
                servico.isAtivo(),
                servico.getCriadoEm()
        );
    }

    public List<ServicoResponse> present(List<Servico> servicos) {
        return servicos.stream().map(this::present).toList();
    }
}
