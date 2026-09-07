package br.com.oficina.servico.usecases;

import br.com.oficina.servico.entities.Servico;
import br.com.oficina.servico.gateways.ServicoGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastrarServicoUseCase {

    private final ServicoGateway servicoGateway;

    public CadastrarServicoUseCase(ServicoGateway servicoGateway) {
        this.servicoGateway = servicoGateway;
    }

    @Transactional
    public Servico execute(CadastrarServicoCommand command) {
        Servico servico = Servico.novo(
                command.nome(), command.descricao(), command.preco(), command.tempoEstimadoMinutos());
        return servicoGateway.save(servico);
    }
}
