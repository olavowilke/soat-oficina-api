package br.com.oficina.veiculo.usecases;

import br.com.oficina.cliente.gateways.ClienteGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import br.com.oficina.veiculo.entities.Veiculo;
import br.com.oficina.veiculo.gateways.VeiculoGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ListarVeiculosPorClienteUseCase {

    private final VeiculoGateway veiculoGateway;
    private final ClienteGateway clienteGateway;

    public ListarVeiculosPorClienteUseCase(VeiculoGateway veiculoGateway, ClienteGateway clienteGateway) {
        this.veiculoGateway = veiculoGateway;
        this.clienteGateway = clienteGateway;
    }

    @Transactional(readOnly = true)
    public List<Veiculo> execute(UUID clienteId) {
        if (!clienteGateway.existsById(clienteId)) {
            throw new RecursoNaoEncontradoException("Cliente", clienteId);
        }
        return veiculoGateway.findByClienteId(clienteId);
    }
}
