package br.com.oficina.peca.usecases;

import br.com.oficina.peca.entities.Peca;
import br.com.oficina.peca.gateways.PecaGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AjustarEstoquePecaUseCase {

    private final PecaGateway pecaGateway;

    public AjustarEstoquePecaUseCase(PecaGateway pecaGateway) {
        this.pecaGateway = pecaGateway;
    }

    @Transactional
    public Peca execute(UUID id, int quantidade) {
        Peca peca = pecaGateway.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça", id));
        peca.ajustarEstoque(quantidade);
        return pecaGateway.save(peca);
    }
}
