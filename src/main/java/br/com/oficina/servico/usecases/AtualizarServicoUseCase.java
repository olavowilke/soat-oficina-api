package br.com.oficina.servico.usecases;

import br.com.oficina.servico.entities.Servico;
import br.com.oficina.servico.gateways.ServicoGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AtualizarServicoUseCase {

    private final ServicoGateway servicoGateway;

    public AtualizarServicoUseCase(ServicoGateway servicoGateway) {
        this.servicoGateway = servicoGateway;
    }

    @Transactional
    public Servico execute(UUID id, AtualizarServicoCommand command) {
        Servico servico = servicoGateway.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", id));
        servico.atualizar(command.nome(), command.descricao(), command.preco(), command.tempoEstimadoMinutos());
        return servicoGateway.save(servico);
    }
}
