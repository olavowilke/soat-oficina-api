package br.com.oficina.peca.usecases;

import br.com.oficina.peca.entities.Peca;
import br.com.oficina.peca.gateways.PecaGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastrarPecaUseCase {

    private final PecaGateway pecaGateway;

    public CadastrarPecaUseCase(PecaGateway pecaGateway) {
        this.pecaGateway = pecaGateway;
    }

    @Transactional
    public Peca execute(CadastrarPecaCommand command) {
        Peca peca = Peca.novo(command.nome(), command.descricao(),
                command.precoUnitario(), command.quantidadeEstoqueInicial());
        return pecaGateway.save(peca);
    }
}
