package br.com.oficina.veiculo.usecases;

import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import br.com.oficina.veiculo.gateways.VeiculoGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RemoverVeiculoUseCase {

    private final VeiculoGateway veiculoGateway;

    public RemoverVeiculoUseCase(VeiculoGateway veiculoGateway) {
        this.veiculoGateway = veiculoGateway;
    }

    @Transactional
    public void execute(UUID id) {
        if (!veiculoGateway.existsById(id)) {
            throw new RecursoNaoEncontradoException("Veículo", id);
        }
        veiculoGateway.deleteById(id);
    }
}
