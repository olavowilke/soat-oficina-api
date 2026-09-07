package br.com.oficina.veiculo.presenters;

import br.com.oficina.veiculo.entities.Veiculo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Presenter do contexto de veículos: converte a saída dos Use Cases ({@link Veiculo})
 * no ViewModel {@link VeiculoResponse}. Não conhece HTTP/{@code ResponseEntity}.
 */
@Component
public class VeiculoPresenter {

    public VeiculoResponse present(Veiculo veiculo) {
        return new VeiculoResponse(
                veiculo.getId(),
                veiculo.getClienteId(),
                veiculo.getPlaca().valor(),
                veiculo.getMarca(),
                veiculo.getModelo(),
                veiculo.getAno(),
                veiculo.getCor(),
                veiculo.getCriadoEm()
        );
    }

    public List<VeiculoResponse> present(List<Veiculo> veiculos) {
        return veiculos.stream().map(this::present).toList();
    }
}
