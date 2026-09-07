package br.com.oficina.peca.usecases;

import br.com.oficina.peca.entities.Peca;
import br.com.oficina.peca.gateways.PecaGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BuscarPecaUseCase {

    private final PecaGateway pecaGateway;

    public BuscarPecaUseCase(PecaGateway pecaGateway) {
        this.pecaGateway = pecaGateway;
    }

    @Transactional(readOnly = true)
    public Peca execute(UUID id) {
        return pecaGateway.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça", id));
    }
}
