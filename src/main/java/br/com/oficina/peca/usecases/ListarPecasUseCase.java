package br.com.oficina.peca.usecases;

import br.com.oficina.peca.entities.Peca;
import br.com.oficina.peca.gateways.PecaGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListarPecasUseCase {

    private final PecaGateway pecaGateway;

    public ListarPecasUseCase(PecaGateway pecaGateway) {
        this.pecaGateway = pecaGateway;
    }

    @Transactional(readOnly = true)
    public List<Peca> execute() {
        return pecaGateway.findAllAtivos();
    }
}
