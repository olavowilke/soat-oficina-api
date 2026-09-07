package br.com.oficina.servico.usecases;

import br.com.oficina.servico.entities.Servico;
import br.com.oficina.servico.gateways.ServicoGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RemoverServicoUseCase {

    private final ServicoGateway servicoGateway;

    public RemoverServicoUseCase(ServicoGateway servicoGateway) {
        this.servicoGateway = servicoGateway;
    }

    @Transactional
    public void execute(UUID id) {
        Servico servico = servicoGateway.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", id));
        servico.desativar();
        servicoGateway.save(servico);
    }
}
