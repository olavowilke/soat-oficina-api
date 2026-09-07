package br.com.oficina.servico.usecases;

import br.com.oficina.servico.entities.Servico;
import br.com.oficina.servico.gateways.ServicoGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BuscarServicoUseCase {

    private final ServicoGateway servicoGateway;

    public BuscarServicoUseCase(ServicoGateway servicoGateway) {
        this.servicoGateway = servicoGateway;
    }

    @Transactional(readOnly = true)
    public Servico execute(UUID id) {
        return servicoGateway.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", id));
    }
}
