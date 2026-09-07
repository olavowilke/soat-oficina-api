package br.com.oficina.peca.usecases;

import br.com.oficina.peca.entities.Peca;
import br.com.oficina.peca.gateways.PecaGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RemoverPecaUseCase {

    private final PecaGateway pecaGateway;

    public RemoverPecaUseCase(PecaGateway pecaGateway) {
        this.pecaGateway = pecaGateway;
    }

    @Transactional
    public void execute(UUID id) {
        Peca peca = pecaGateway.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça", id));
        peca.desativar();
        pecaGateway.save(peca);
    }
}
