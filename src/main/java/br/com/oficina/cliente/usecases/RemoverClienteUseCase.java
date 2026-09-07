package br.com.oficina.cliente.usecases;

import br.com.oficina.cliente.entities.Cliente;
import br.com.oficina.cliente.gateways.ClienteGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RemoverClienteUseCase {

    private final ClienteGateway clienteGateway;

    public RemoverClienteUseCase(ClienteGateway clienteGateway) {
        this.clienteGateway = clienteGateway;
    }

    @Transactional
    public void execute(UUID id) {
        Cliente cliente = clienteGateway.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
        cliente.desativar();
        clienteGateway.save(cliente);
    }
}
