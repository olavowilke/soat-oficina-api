package br.com.oficina.cliente.usecases;

import br.com.oficina.cliente.entities.Cliente;
import br.com.oficina.cliente.gateways.ClienteGateway;
import br.com.oficina.shared.domain.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AtualizarClienteUseCase {

    private final ClienteGateway clienteGateway;

    public AtualizarClienteUseCase(ClienteGateway clienteGateway) {
        this.clienteGateway = clienteGateway;
    }

    @Transactional
    public Cliente execute(UUID id, AtualizarClienteCommand command) {
        Cliente cliente = clienteGateway.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
        cliente.atualizar(command.nome(), command.email(), command.telefone());
        return clienteGateway.save(cliente);
    }
}
