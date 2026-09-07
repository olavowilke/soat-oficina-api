package br.com.oficina.ordemservico.usecases;

import br.com.oficina.cliente.gateways.ClienteGateway;
import br.com.oficina.ordemservico.entities.ItemPeca;
import br.com.oficina.ordemservico.entities.ItemServico;
import br.com.oficina.ordemservico.entities.OrdemServico;
import br.com.oficina.ordemservico.gateways.OrdemServicoGateway;
import br.com.oficina.peca.entities.Peca;
import br.com.oficina.peca.gateways.PecaGateway;
import br.com.oficina.servico.entities.Servico;
import br.com.oficina.servico.gateways.ServicoGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import br.com.oficina.veiculo.gateways.VeiculoGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abertura consolidada de Ordem de Serviço: numa única operação transacional
 * resolve cliente e veículo (por ID já cadastrado), adiciona os serviços e as
 * peças informados (reservando o estoque) e persiste a OS, retornando o agregado
 * já com o orçamento calculado. Se qualquer item falhar (ex.: estoque
 * insuficiente), toda a operação é revertida.
 */
@Service
public class AbrirOrdemServicoUseCase {

    private final OrdemServicoGateway ordemServicoGateway;
    private final ServicoGateway servicoGateway;
    private final PecaGateway pecaGateway;
    private final ClienteGateway clienteGateway;
    private final VeiculoGateway veiculoGateway;

    public AbrirOrdemServicoUseCase(OrdemServicoGateway ordemServicoGateway,
                                    ServicoGateway servicoGateway,
                                    PecaGateway pecaGateway,
                                    ClienteGateway clienteGateway,
                                    VeiculoGateway veiculoGateway) {
        this.ordemServicoGateway = ordemServicoGateway;
        this.servicoGateway = servicoGateway;
        this.pecaGateway = pecaGateway;
        this.clienteGateway = clienteGateway;
        this.veiculoGateway = veiculoGateway;
    }

    @Transactional
    public OrdemServico execute(AbrirOrdemServicoCommand command) {
        if (!clienteGateway.existsById(command.clienteId())) {
            throw new RecursoNaoEncontradoException("Cliente", command.clienteId());
        }
        if (!veiculoGateway.existsById(command.veiculoId())) {
            throw new RecursoNaoEncontradoException("Veículo", command.veiculoId());
        }

        OrdemServico os = OrdemServico.nova(command.clienteId(), command.veiculoId(), command.observacoes());

        for (var servicoId : command.servicoIds()) {
            Servico servico = servicoGateway.findById(servicoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", servicoId));
            os.adicionarServico(ItemServico.novo(servico.getId(), servico.getNome(), servico.getPreco()));
        }

        for (ItemPecaInput linha : command.pecas()) {
            Peca peca = pecaGateway.findById(linha.pecaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Peça", linha.pecaId()));
            peca.ajustarEstoque(-linha.quantidade());
            pecaGateway.save(peca);
            os.adicionarPeca(ItemPeca.novo(peca.getId(), peca.getNome(), linha.quantidade(), peca.getPrecoUnitario()));
        }

        return ordemServicoGateway.save(os);
    }
}
