package br.com.oficina.servico.usecases;

import br.com.oficina.servico.entities.Servico;
import br.com.oficina.servico.gateways.ServicoGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListarServicosUseCase {

    private final ServicoGateway servicoGateway;

    public ListarServicosUseCase(ServicoGateway servicoGateway) {
        this.servicoGateway = servicoGateway;
    }

    @Transactional(readOnly = true)
    public List<Servico> execute() {
        return servicoGateway.findAllAtivos();
    }
}
