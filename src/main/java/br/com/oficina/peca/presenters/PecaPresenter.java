package br.com.oficina.peca.presenters;

import br.com.oficina.peca.entities.Peca;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Presenter do contexto de peças: converte a saída dos Use Cases ({@link Peca})
 * no ViewModel {@link PecaResponse}. Não conhece HTTP/{@code ResponseEntity}.
 */
@Component
public class PecaPresenter {

    public PecaResponse present(Peca peca) {
        return new PecaResponse(
                peca.getId(),
                peca.getNome(),
                peca.getDescricao(),
                peca.getPrecoUnitario(),
                peca.getQuantidadeEstoque(),
                peca.isAtivo(),
                peca.getCriadoEm()
        );
    }

    public List<PecaResponse> present(List<Peca> pecas) {
        return pecas.stream().map(this::present).toList();
    }
}
