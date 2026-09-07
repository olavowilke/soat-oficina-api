package br.com.oficina.veiculo.usecases;

import br.com.oficina.cliente.gateways.ClienteGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import br.com.oficina.veiculo.entities.Placa;
import br.com.oficina.veiculo.entities.Veiculo;
import br.com.oficina.veiculo.gateways.VeiculoGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastrarVeiculoUseCase {

    private final VeiculoGateway veiculoGateway;
    private final ClienteGateway clienteGateway;

    public CadastrarVeiculoUseCase(VeiculoGateway veiculoGateway, ClienteGateway clienteGateway) {
        this.veiculoGateway = veiculoGateway;
        this.clienteGateway = clienteGateway;
    }

    @Transactional
    public Veiculo execute(CadastrarVeiculoCommand command) {
        if (!clienteGateway.existsById(command.clienteId())) {
            throw new RecursoNaoEncontradoException("Cliente", command.clienteId());
        }
        Placa placa = new Placa(command.placa());
        if (veiculoGateway.existsByPlaca(placa.valor())) {
            throw new IllegalArgumentException("Já existe um veículo com a placa informada");
        }
        Veiculo veiculo = Veiculo.novo(command.clienteId(), placa, command.marca(),
                command.modelo(), command.ano(), command.cor());
        return veiculoGateway.save(veiculo);
    }
}
