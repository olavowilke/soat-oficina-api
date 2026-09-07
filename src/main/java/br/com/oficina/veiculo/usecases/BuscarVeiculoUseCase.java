package br.com.oficina.veiculo.usecases;

import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import br.com.oficina.veiculo.entities.Veiculo;
import br.com.oficina.veiculo.gateways.VeiculoGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BuscarVeiculoUseCase {

    private final VeiculoGateway veiculoGateway;

    public BuscarVeiculoUseCase(VeiculoGateway veiculoGateway) {
        this.veiculoGateway = veiculoGateway;
    }

    @Transactional(readOnly = true)
    public Veiculo execute(UUID id) {
        return veiculoGateway.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo", id));
    }
}
