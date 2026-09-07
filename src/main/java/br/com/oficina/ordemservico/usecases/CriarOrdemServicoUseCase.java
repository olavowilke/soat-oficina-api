package br.com.oficina.ordemservico.usecases;

import br.com.oficina.cliente.gateways.ClienteGateway;
import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import br.com.oficina.veiculo.gateways.VeiculoGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CriarOrdemServicoUseCase {

    private final OrdemServicoGateway ordemServicoGateway;
    private final ClienteGateway clienteGateway;
    private final VeiculoGateway veiculoGateway;

    public CriarOrdemServicoUseCase(OrdemServicoGateway ordemServicoGateway,
                                    ClienteGateway clienteGateway,
                                    VeiculoGateway veiculoGateway) {
        this.ordemServicoGateway = ordemServicoGateway;
        this.clienteGateway = clienteGateway;
        this.veiculoGateway = veiculoGateway;
    }

    @Transactional
    public OrdemServico execute(CriarOrdemServicoCommand command) {
        if (!clienteGateway.existsById(command.clienteId())) {
            throw new RecursoNaoEncontradoException("Cliente", command.clienteId());
        }
        if (!veiculoGateway.existsById(command.veiculoId())) {
            throw new RecursoNaoEncontradoException("Veículo", command.veiculoId());
        }
        OrdemServico os = OrdemServico.nova(command.clienteId(), command.veiculoId(), command.observacoes());
        return ordemServicoGateway.save(os);
    }
}
