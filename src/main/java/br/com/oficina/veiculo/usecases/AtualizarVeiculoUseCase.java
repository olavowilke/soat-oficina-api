package br.com.oficina.veiculo.usecases;

import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import br.com.oficina.veiculo.entities.Veiculo;
import br.com.oficina.veiculo.gateways.VeiculoGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AtualizarVeiculoUseCase {

    private final VeiculoGateway veiculoGateway;

    public AtualizarVeiculoUseCase(VeiculoGateway veiculoGateway) {
        this.veiculoGateway = veiculoGateway;
    }

    @Transactional
    public Veiculo execute(UUID id, AtualizarVeiculoCommand command) {
        Veiculo veiculo = veiculoGateway.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo", id));
        veiculo.atualizar(command.marca(), command.modelo(), command.ano(), command.cor());
        return veiculoGateway.save(veiculo);
    }
}
