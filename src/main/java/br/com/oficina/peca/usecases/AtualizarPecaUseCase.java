package br.com.oficina.peca.usecases;

import br.com.oficina.peca.entities.Peca;
import br.com.oficina.peca.gateways.PecaGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AtualizarPecaUseCase {

    private final PecaGateway pecaGateway;

    public AtualizarPecaUseCase(PecaGateway pecaGateway) {
        this.pecaGateway = pecaGateway;
    }

    @Transactional
    public Peca execute(UUID id, AtualizarPecaCommand command) {
        Peca peca = pecaGateway.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça", id));
        peca.atualizar(command.nome(), command.descricao(), command.precoUnitario());
        return pecaGateway.save(peca);
    }
}
